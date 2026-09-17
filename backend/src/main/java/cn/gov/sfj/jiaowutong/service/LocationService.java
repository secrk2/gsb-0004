package cn.gov.sfj.jiaowutong.service;

import cn.gov.sfj.jiaowutong.common.ApiException;
import cn.gov.sfj.jiaowutong.domain.ObjStatus;
import cn.gov.sfj.jiaowutong.domain.ViolationType;
import cn.gov.sfj.jiaowutong.dto.Dtos.*;
import cn.gov.sfj.jiaowutong.model.*;
import cn.gov.sfj.jiaowutong.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/**
 * 定位受理：
 * 1. 幂等：(object, clientId) 唯一，重复/重试不产生第二条轨迹点；
 * 2. 反作弊：在线实时上报的定位时间戳必须接近当前时间，旧位置直接拒绝并说明原因；
 *    断网补报允许携带真实的历史 recordedAt（离线队列），按 recordedAt 升序合并；
 * 3. 合并：与最新轨迹点位移小于阈值视为位置未变更，不落点（MERGED），不产生重复轨迹。
 */
@Service
@RequiredArgsConstructor
public class LocationService {

    private final LocationPointRepository pointRepository;
    private final LocationIngestRepository ingestRepository;
    private final CorrectionObjectRepository objectRepository;
    private final ViolationRepository violationRepository;

    @Value("${app.location.merge-distance-meters:8}")
    private int mergeDistanceMeters;

    @Value("${app.location.fresh-tolerance-seconds:300}")
    private int freshToleranceSeconds;

    @Transactional
    public SyncBatchResp report(AppUser user, LocationReportReq req) {
        CorrectionObject obj = requireSelf(user);
        validate(req);
        SyncItemResult r = ingest(obj, req, "ONLINE");
        int stored = "STORED".equals(r.outcome()) ? 1 : 0;
        int merged = "MERGED".equals(r.outcome()) ? 1 : 0;
        int rejected = "REJECTED".equals(r.outcome()) ? 1 : 0;
        return new SyncBatchResp(1, stored, merged, rejected, List.of(r));
    }

    /** 断网恢复：批量补报。按定位真实时间升序处理，逐条幂等+合并。 */
    @Transactional
    public SyncBatchResp sync(AppUser user, SyncBatchReq req) {
        CorrectionObject obj = requireSelf(user);
        if (req == null || req.points() == null || req.points().isEmpty()) {
            throw ApiException.badRequest("补报队列不能为空");
        }
        List<LocationReportReq> ordered = req.points().stream()
                .filter(p -> p != null && p.recordedAt() != null)
                .sorted(Comparator.comparing(LocationReportReq::recordedAt))
                .toList();
        int stored = 0, merged = 0, rejected = 0;
        java.util.List<SyncItemResult> results = new java.util.ArrayList<>();
        for (LocationReportReq p : ordered) {
            try {
                SyncItemResult r = ingest(obj, p, "OFFLINE_SYNC");
                results.add(r);
                if ("STORED".equals(r.outcome())) stored++;
                else if ("MERGED".equals(r.outcome())) merged++;
                else if ("REJECTED".equals(r.outcome())) rejected++;
            } catch (ApiException e) {
                results.add(new SyncItemResult(p.clientId(), "REJECTED", null, e.getMessage()));
                rejected++;
            }
        }
        return new SyncBatchResp(ordered.size(), stored, merged, rejected, results);
    }

    @Transactional(readOnly = true)
    public List<LocationPointDto> track(AppUser user, Long objectId) {
        CorrectionObject obj;
        if (user.getRole() == cn.gov.sfj.jiaowutong.domain.UserRole.OBJECT) {
            obj = requireSelf(user);
        } else {
            obj = objectRepository.findById(objectId)
                    .orElseThrow(() -> ApiException.notFound("对象档案不存在"));
            if (user.getOffice() != null
                    && !obj.getOffice().getId().equals(user.getOffice().getId())) {
                throw ApiException.forbidden("越权访问被拦截：该对象不在您所在司法所管辖范围内");
            }
        }
        return pointRepository.findTop200ByObjectIdOrderByRecordedAtDesc(obj.getId()).stream()
                .map(p -> new LocationPointDto(p.getId(), p.getLat(), p.getLng(), p.getAccuracy(),
                        p.getRecordedAt(), p.getReportedAt(), p.getSource()))
                .toList();
    }

    // ---------- core ----------

    private SyncItemResult ingest(CorrectionObject obj, LocationReportReq req, String source) {
        validate(req);

        // 1) 幂等：同一 clientId 重传/补传，原样返回首次结论，不重复落点
        var existed = ingestRepository.findByObjectIdAndClientId(obj.getId(), req.clientId());
        if (existed.isPresent()) {
            LocationIngest old = existed.get();
            // 本次调用结果记为 IDEMPOTENT（未新增任何轨迹点），历史结论放在 message 中
            return new SyncItemResult(req.clientId(), "IDEMPOTENT", old.getPointId(),
                    "该定位点已受理过（首次结论：" + outcomeText(old.getOutcome())
                            + "），按幂等处理，未重复生成轨迹点");
        }

        Instant now = Instant.now();
        Instant at = req.recordedAt();

        // 2) 旧位置糊弄检测
        if ("ONLINE".equals(source)) {
            long drift = Math.abs(Duration.between(at, now).getSeconds());
            if (drift > freshToleranceSeconds) {
                registerIngest(obj, req.clientId(), "LATE", null);
                return new SyncItemResult(req.clientId(), "REJECTED", null,
                        "定位时间与当前时间相差 " + drift + " 秒，疑似旧位置/修改时间，实时上报被拒绝");
            }
        } else {
            // 离线补报允许历史时间，但不接受未来时间或超过30天的陈旧点
            if (at.isAfter(now.plusSeconds(freshToleranceSeconds))) {
                registerIngest(obj, req.clientId(), "LATE", null);
                return new SyncItemResult(req.clientId(), "REJECTED", null,
                        "定位时间晚于当前时间，时间不合法，已拒绝");
            }
            if (at.isBefore(now.minus(Duration.ofDays(30)))) {
                registerIngest(obj, req.clientId(), "LATE", null);
                return new SyncItemResult(req.clientId(), "REJECTED", null,
                        "补报点超过30天保留期，已拒绝");
            }
        }

        // 3) 位置未变更合并：与最新轨迹点比较
        LocationPoint latest = pointRepository
                .findFirstByObjectIdOrderByRecordedAtDesc(obj.getId()).orElse(null);
        if (latest != null) {
            double d = cn.gov.sfj.jiaowutong.util.GeoUtils.distanceMeters(
                    latest.getLat(), latest.getLng(), req.lat(), req.lng());
            if (d < mergeDistanceMeters) {
                LocationIngest ing = registerIngest(obj, req.clientId(), "MERGED", latest.getId());
                return new SyncItemResult(req.clientId(), "MERGED", latest.getId(),
                        "与上一轨迹点仅相距 " + Math.round(d) + " 米，位置未变更，已合并（不新增轨迹点）");
            }
        }

        LocationPoint p = new LocationPoint();
        p.setObject(obj);
        p.setLat(req.lat());
        p.setLng(req.lng());
        p.setAccuracy(req.accuracy());
        p.setRecordedAt(at);
        p.setReportedAt(now);
        p.setSource(source);
        pointRepository.save(p);
        registerIngest(obj, req.clientId(), "STORED", p.getId());

        // 4) 电子围栏越界判定（仅在矫状态；请假外出对象在批准目的地不判定）
        if (obj.getStatus() == ObjStatus.ACTIVE
                && obj.getHomeLat() != null && obj.getHomeLng() != null
                && obj.getFenceRadiusMeters() != null) {
            double dist = cn.gov.sfj.jiaowutong.util.GeoUtils.distanceMeters(
                    obj.getHomeLat(), obj.getHomeLng(), req.lat(), req.lng());
            if (dist > obj.getFenceRadiusMeters()
                    && !violationRepository.existsByObjectIdAndTypeAndHandledFalse(obj.getId(),
                            ViolationType.BOUNDARY)) {
                Violation v = new Violation();
                v.setObject(obj);
                v.setType(ViolationType.BOUNDARY);
                v.setLat(req.lat());
                v.setLng(req.lng());
                v.setOccurredAt(at);
                v.setDetail("定位偏离法定活动范围 " + Math.round(dist) + " 米（围栏半径 "
                        + obj.getFenceRadiusMeters() + " 米），疑似越界"
                        + ("OFFLINE_SYNC".equals(source) ? "（断网补报点）" : ""));
                violationRepository.save(v);
            }
        }
        return new SyncItemResult(req.clientId(), "STORED", p.getId(), "已记录轨迹点");
    }

    private LocationIngest registerIngest(CorrectionObject obj, String clientId,
                                          String outcome, Long pointId) {
        LocationIngest ing = new LocationIngest();
        ing.setObject(obj);
        ing.setClientId(clientId);
        ing.setOutcome(outcome);
        ing.setPointId(pointId);
        return ingestRepository.save(ing);
    }

    private String outcomeText(String outcome) {
        return switch (outcome) {
            case "STORED" -> "已新增轨迹点";
            case "MERGED" -> "位置未变更已合并";
            case "LATE" -> "时间异常";
            default -> outcome;
        };
    }

    private void validate(LocationReportReq req) {
        if (req == null) {
            throw ApiException.badRequest("缺少定位数据");
        }
        if (req.clientId() == null || req.clientId().isBlank()) {
            throw ApiException.badRequest("缺少定位点唯一编号 clientId，无法保证幂等");
        }
        if (req.lat() == null || req.lng() == null) {
            throw ApiException.badRequest("缺少经纬度");
        }
        if (Math.abs(req.lat()) > 90 || Math.abs(req.lng()) > 180) {
            throw ApiException.badRequest("经纬度数值不合法");
        }
        if (req.recordedAt() == null) {
            throw ApiException.badRequest("缺少定位时间，不能用缓存的旧位置上报");
        }
    }

    private CorrectionObject requireSelf(AppUser user) {        if (user.getRole() != cn.gov.sfj.jiaowutong.domain.UserRole.OBJECT
                || user.getObjectId() == null) {
            throw ApiException.forbidden("仅矫正对象本人账号可以上报定位");
        }
        return objectRepository.findById(user.getObjectId())
                .orElseThrow(() -> ApiException.notFound("对象档案不存在"));
    }
}
