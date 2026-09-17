package cn.gov.sfj.jiaowutong.service;

import cn.gov.sfj.jiaowutong.common.ApiException;
import cn.gov.sfj.jiaowutong.domain.ObjStatus;
import cn.gov.sfj.jiaowutong.domain.UserRole;
import cn.gov.sfj.jiaowutong.dto.Dtos.*;
import cn.gov.sfj.jiaowutong.model.*;
import cn.gov.sfj.jiaowutong.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ObjectService {

    private final CorrectionObjectRepository objectRepository;
    private final StatusLogRepository statusLogRepository;
    private final NameViewLogRepository nameViewLogRepository;
    private final ViolationRepository violationRepository;
    private final LeaveRequestRepository leaveRepository;
    private final CheckInRepository checkInRepository;
    private final JudicialOfficeRepository officeRepository;
    private final ObjectMapper mapper;

    @Transactional(readOnly = true)
    public List<ObjectSummaryDto> list(AppUser user) {
        List<CorrectionObject> list;
        if (user.getRole() == UserRole.OBJECT) {
            list = user.getObjectId() == null ? List.of()
                    : objectRepository.findById(user.getObjectId()).map(List::of).orElse(List.of());
        } else if (user.getOffice() == null) {
            list = objectRepository.findAll();
        } else {
            list = objectRepository.findByOfficeIdOrderByCode(user.getOffice().getId());
        }
        return list.stream().map(mapper::toSummary).toList();
    }

    /**
     * 取对象并做隔离校验：
     * 对象不存在 -> 404；对象存在但跨所/跨人 -> 403（前端呈现错误态，而不是空白页）。
     */
    @Transactional(readOnly = true)
    public CorrectionObject loadOwned(AppUser user, Long id) {
        CorrectionObject obj = objectRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("未找到编号对应的矫正对象，档案可能已被迁移或删除"));
        if (user.getRole() == UserRole.OBJECT) {
            if (user.getObjectId() == null || !user.getObjectId().equals(id)) {
                throw ApiException.forbidden("无权访问：这是其他矫正对象的档案，系统已记录本次越权尝试");
            }
        } else if (user.getOffice() != null
                && !obj.getOffice().getId().equals(user.getOffice().getId())) {
            throw ApiException.forbidden("越权访问被拦截：对象「" + mapper.maskedName(obj)
                    + "」隶属" + obj.getOffice().getName() + "，不在您所在"
                    + user.getOffice().getName() + "的管辖范围内");
        }
        return obj;
    }

    @Transactional(readOnly = true)
    public ObjectDetailDto detail(AppUser user, Long id) {
        CorrectionObject obj = loadOwned(user, id);
        boolean reveal = user.getRole() == UserRole.OBJECT;
        return mapper.toDetail(obj, reveal);
    }

    @Transactional
    public ObjectDetailDto changeStatus(AppUser user, Long id, StatusChangeReq req) {
        AppUser staff = requireStaff(user);
        CorrectionObject obj = loadOwned(staff, id);
        if (req == null || req.toStatus() == null || req.toStatus().isBlank()) {
            throw ApiException.badRequest("请选择要流转的目标状态");
        }
        ObjStatus target;
        try {
            target = ObjStatus.valueOf(req.toStatus().trim());
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("目标状态不存在：" + req.toStatus());
        }
        ObjStatus from = obj.getStatus();
        if (!StateMachine.canTransit(from, target)) {
            // 非法回退必须拦下并告知原因
            throw ApiException.badRequest(StateMachine.explain(from, target));
        }
        String reason = req.reason() == null ? "" : req.reason().trim();
        if (target != ObjStatus.ACTIVE && reason.length() < 2) {
            throw ApiException.badRequest("变更为「" + target.getLabel() + "」必须填写事由（不少于2个字）");
        }

        obj.setStatus(target);
        objectRepository.save(obj);

        StatusLog log = new StatusLog();
        log.setObject(obj);
        log.setFromStatus(from);
        log.setToStatus(target);
        log.setOperatorName(staff.getDisplayName());
        log.setReason(reason);
        statusLogRepository.save(log);

        if (target == ObjStatus.WARNED) {
            Violation v = new Violation();
            v.setObject(obj);
            v.setType(cn.gov.sfj.jiaowutong.domain.ViolationType.WARNING);
            v.setDetail("训诫处理：" + reason);
            v.setOccurredAt(Instant.now());
            violationRepository.save(v);
        }
        return mapper.toDetail(obj, false);
    }

    @Transactional
    public ObjectDetailDto revealName(AppUser user, Long id, RevealNameReq req) {
        AppUser staff = requireStaff(user);
        CorrectionObject obj = loadOwned(staff, id);
        String reason = req == null || req.reason() == null ? "" : req.reason().trim();
        if (reason.length() < 4) {
            throw ApiException.badRequest("查看全名属于敏感操作，请填写不少于4个字的查阅理由");
        }
        NameViewLog log = new NameViewLog();
        log.setObject(obj);
        log.setViewerName(staff.getDisplayName());
        log.setViewerRole(staff.getRole().name());
        log.setReason(reason);
        nameViewLogRepository.save(log);
        return mapper.toDetail(obj, true);
    }

    @Transactional(readOnly = true)
    public List<TimelineEvent> timeline(AppUser user, Long id) {
        CorrectionObject obj = loadOwned(user, id);
        List<TimelineEvent> events = new ArrayList<>();
        for (StatusLog l : statusLogRepository.findByObjectIdOrderByCreatedAtDesc(obj.getId())) {
            String title = (l.getFromStatus() == null ? "建档" : l.getFromStatus().getLabel())
                    + " → " + l.getToStatus().getLabel();
            events.add(new TimelineEvent("STATUS", title, l.getReason(), l.getOperatorName(), l.getCreatedAt()));
        }
        for (Violation v : violationRepository.findByObjectIdOrderByOccurredAtDesc(obj.getId())) {
            events.add(new TimelineEvent("VIOLATION", v.getType().getLabel(),
                    v.getDetail() + (v.isHandled() ? "（已处理）" : "（未处理）"),
                    null, v.getOccurredAt()));
        }
        for (LeaveRequest l : leaveRepository.findByObjectIdOrderByCreatedAtDesc(obj.getId())) {
            events.add(new TimelineEvent("LEAVE", "请假-" + leaveStatusText(l.getStatus()),
                    l.getDestination() + "｜" + l.getReason(),
                    l.getApprover(), l.getCreatedAt()));
        }
        return events.stream()
                .sorted(Comparator.comparing(TimelineEvent::at).reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NameViewLog> nameViewLogs(AppUser user, Long id) {
        AppUser staff = requireStaff(user);
        loadOwned(staff, id);
        return nameViewLogRepository.findByObjectIdOrderByCreatedAtDesc(id);
    }

    // ---------- 请假 ----------

    @Transactional
    public LeaveDto applyLeave(AppUser user, Long id, LeaveReq req) {
        CorrectionObject obj;
        if (user.getRole() == UserRole.OBJECT) {
            if (user.getObjectId() == null || !user.getObjectId().equals(id)) {
                throw ApiException.forbidden("只能为本人提交请假申请");
            }
            obj = objectRepository.findById(id).orElseThrow();
        } else {
            obj = loadOwned(user, id);
        }
        if (obj.getStatus() != ObjStatus.ACTIVE) {
            throw ApiException.badRequest("当前状态为「" + obj.getStatus().getLabel()
                    + "」，只有在矫对象可以申请请假外出");
        }
        if (req == null || req.reason() == null || req.reason().trim().length() < 2) {
            throw ApiException.badRequest("请填写请假事由");
        }
        if (req.startDate() == null || req.endDate() == null || req.endDate().isBefore(req.startDate())) {
            throw ApiException.badRequest("请假起止日期不合法");
        }
        LeaveRequest leave = new LeaveRequest();
        leave.setObject(obj);
        leave.setReason(req.reason().trim());
        leave.setDestination(req.destination());
        leave.setStartDate(req.startDate());
        leave.setEndDate(req.endDate());
        leave.setStatus("PENDING");
        leaveRepository.save(leave);
        return toLeaveDto(leave);
    }

    @Transactional
    public LeaveDto decideLeave(AppUser user, Long leaveId, boolean approve, LeaveDecisionReq req) {
        AppUser staff = requireStaff(user);
        LeaveRequest leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> ApiException.notFound("请假申请不存在"));
        CorrectionObject obj = leave.getObject();
        if (user.getOffice() != null && !obj.getOffice().getId().equals(user.getOffice().getId())) {
            throw ApiException.forbidden("越权操作被拦截：该请假申请不属于您所在的司法所");
        }
        if (!"PENDING".equals(leave.getStatus())) {
            throw ApiException.badRequest("该请假申请已" + leaveStatusText(leave.getStatus()) + "，不能重复审批");
        }
        leave.setStatus(approve ? "APPROVED" : "REJECTED");
        leave.setApprover(staff.getDisplayName());
        leaveRepository.save(leave);

        if (approve) {
            if (!StateMachine.canTransit(obj.getStatus(), ObjStatus.LEAVE)) {
                throw ApiException.badRequest(StateMachine.explain(obj.getStatus(), ObjStatus.LEAVE));
            }
            ObjStatus from = obj.getStatus();
            obj.setStatus(ObjStatus.LEAVE);
            objectRepository.save(obj);
            StatusLog log = new StatusLog();
            log.setObject(obj);
            log.setFromStatus(from);
            log.setToStatus(ObjStatus.LEAVE);
            log.setOperatorName(staff.getDisplayName());
            log.setReason("批准请假外出：" + leave.getDestination() + "，"
                    + leave.getStartDate() + " 至 " + leave.getEndDate()
                    + (req != null && req.note() != null ? "；" + req.note().trim() : ""));
            statusLogRepository.save(log);
        }
        return toLeaveDto(leave);
    }

    /** 对象销假：请假外出 -> 在矫 */
    @Transactional
    public ObjectDetailDto returnFromLeave(AppUser user) {
        if (user.getRole() != UserRole.OBJECT || user.getObjectId() == null) {
            throw ApiException.forbidden("仅矫正对象本人可以销假");
        }
        CorrectionObject obj = objectRepository.findById(user.getObjectId())
                .orElseThrow(() -> ApiException.notFound("对象档案不存在"));
        if (obj.getStatus() != ObjStatus.LEAVE) {
            throw ApiException.badRequest("当前不是请假外出状态，无需销假");
        }
        obj.setStatus(ObjStatus.ACTIVE);
        objectRepository.save(obj);
        StatusLog log = new StatusLog();
        log.setObject(obj);
        log.setFromStatus(ObjStatus.LEAVE);
        log.setToStatus(ObjStatus.ACTIVE);
        log.setOperatorName(obj.getFullName() + "（本人销假）");
        log.setReason("对象假期届满，线上销假");
        statusLogRepository.save(log);
        for (LeaveRequest l : leaveRepository.findByObjectIdOrderByCreatedAtDesc(obj.getId())) {
            if ("APPROVED".equals(l.getStatus())) {
                l.setStatus("RETURNED");
                leaveRepository.save(l);
                break;
            }
        }
        return mapper.toDetail(obj, true);
    }

    @Transactional(readOnly = true)
    public List<LeaveDto> leaves(AppUser user, Long id) {
        CorrectionObject obj = loadOwned(user, id);
        return leaveRepository.findByObjectIdOrderByCreatedAtDesc(obj.getId()).stream()
                .map(this::toLeaveDto).toList();
    }

    // ---------- 报到 ----------

    @Transactional
    public void checkIn(AppUser user) {
        if (user.getRole() != UserRole.OBJECT || user.getObjectId() == null) {
            throw ApiException.forbidden("仅矫正对象本人可以报到");
        }
        CorrectionObject obj = objectRepository.findById(user.getObjectId())
                .orElseThrow(() -> ApiException.notFound("对象档案不存在"));
        LocalDate today = LocalDate.now();
        CheckIn checkIn = checkInRepository
                .findByObjectIdAndDueDateAndStatus(obj.getId(), today, "DUE")
                .orElse(null);
        if (checkIn == null) {
            // 没有待报到记录时，若今日已报到则拒绝重复报到；否则补建今日记录
            boolean doneToday = checkInRepository
                    .findByObjectIdAndDueDateLessThanEqualOrderByDueDateDesc(obj.getId(), today).stream()
                    .anyMatch(c -> today.equals(c.getDueDate()) && "DONE".equals(c.getStatus()));
            if (doneToday) {
                throw ApiException.badRequest("今日已完成报到，无需重复报到");
            }
            checkIn = new CheckIn();
            checkIn.setObject(obj);
            checkIn.setDueDate(today);
        }
        checkIn.setStatus("DONE");
        checkIn.setDoneAt(Instant.now());
        checkIn.setMethod("APP");
        checkInRepository.save(checkIn);
    }

    @Transactional(readOnly = true)
    public List<CheckIn> checkInHistory(AppUser user, Long id) {
        CorrectionObject obj = loadOwned(user, id);
        return checkInRepository.findByObjectIdAndDueDateLessThanEqualOrderByDueDateDesc(
                obj.getId(), LocalDate.now());
    }

    // ---------- helpers ----------

    private AppUser requireStaff(AppUser user) {
        if (!AuthService.isStaff(user)) {
            throw ApiException.forbidden("仅监管员/司法所干警可执行该操作");
        }
        return user;
    }

    private LeaveDto toLeaveDto(LeaveRequest l) {
        return new LeaveDto(l.getId(), l.getObject().getId(), l.getReason(), l.getDestination(),
                l.getStartDate(), l.getEndDate(), l.getStatus(),
                l.getApprover(), l.getCreatedAt());
    }

    private String leaveStatusText(String s) {
        return switch (s) {
            case "PENDING" -> "待审批";
            case "APPROVED" -> "已批准";
            case "REJECTED" -> "已驳回";
            case "RETURNED" -> "已销假";
            default -> s;
        };
    }
}
