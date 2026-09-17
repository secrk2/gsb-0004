package cn.gov.sfj.jiaowutong.service;

import cn.gov.sfj.jiaowutong.domain.ObjStatus;
import cn.gov.sfj.jiaowutong.dto.Dtos.*;
import cn.gov.sfj.jiaowutong.model.AppUser;
import cn.gov.sfj.jiaowutong.model.JudicialOffice;
import cn.gov.sfj.jiaowutong.model.Violation;
import cn.gov.sfj.jiaowutong.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/** 矫务作战台：各所在矫漏斗、今日应报到、越界与违规红点 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final JudicialOfficeRepository officeRepository;
    private final CorrectionObjectRepository objectRepository;
    private final CheckInRepository checkInRepository;
    private final ViolationRepository violationRepository;
    private final ObjectMapper mapper;

    @Transactional(readOnly = true)
    public DashboardResp dashboard(AppUser user) {
        LocalDate today = LocalDate.now();
        List<JudicialOffice> allOffices = officeRepository.findAll();
        // 隔离：监管员看全区各所；司法所干警只看本所漏斗
        Long scopeOfficeId = user.getOffice() == null ? null : user.getOffice().getId();
        List<JudicialOffice> offices = scopeOfficeId == null
                ? allOffices
                : allOffices.stream().filter(o -> o.getId().equals(scopeOfficeId)).toList();

        List<OfficeStat> stats = offices.stream().map(o -> {
            Long oid = o.getId();
            long intake = objectRepository.countByOfficeIdAndStatus(oid, ObjStatus.INTAKE);
            long active = objectRepository.countByOfficeIdAndStatus(oid, ObjStatus.ACTIVE);
            long leave = objectRepository.countByOfficeIdAndStatus(oid, ObjStatus.LEAVE);
            long warned = objectRepository.countByOfficeIdAndStatus(oid, ObjStatus.WARNED);
            long revoked = objectRepository.countByOfficeIdAndStatus(oid, ObjStatus.REVOKED);
            long terminated = objectRepository.countByOfficeIdAndStatus(oid, ObjStatus.TERMINATED);
            long due = checkInRepository.countByObjectOfficeIdAndDueDateAndStatus(oid, today, "DUE");
            long overdue = checkInRepository.countByObjectOfficeIdAndDueDateLessThanAndStatus(oid, today, "DUE");
            long openV = violationRepository.countByObjectOfficeIdAndHandledFalse(oid);
            long total = intake + active + leave + warned + revoked + terminated;
            return new OfficeStat(oid, o.getName(), intake, active, leave, warned, revoked,
                    terminated, total, due, overdue, openV);
        }).toList();

        List<Violation> redDotSource = scopeOfficeId == null
                ? violationRepository.findTop20ByHandledFalseOrderByOccurredAtDesc()
                : violationRepository.findTop20ByObjectOfficeIdAndHandledFalseOrderByOccurredAtDesc(scopeOfficeId);
        List<RedDotItem> redDots = redDotSource.stream().map(mapper::toRedDot).toList();

        List<DueItem> dueToday = loadDue(today, scopeOfficeId);
        List<DueItem> overdue = loadOverdue(today, scopeOfficeId);

        return new DashboardResp(stats, dueToday, overdue, redDots);
    }

    private List<DueItem> loadDue(LocalDate today, Long officeId) {
        var checks = officeId == null
                ? checkInRepository.findByDueDateAndStatus(today, "DUE")
                : checkInRepository.findByObjectOfficeIdAndDueDateAndStatus(officeId, today, "DUE");
        return checks.stream()
                .map(c -> new DueItem(c.getId(), c.getObject().getId(),
                        mapper.maskedName(c.getObject()),
                        c.getObject().getStatus().name(), c.getObject().getStatus().getLabel(),
                        c.getObject().getCrimeType()))
                .toList();
    }

    private List<DueItem> loadOverdue(LocalDate today, Long officeId) {
        var checks = officeId == null
                ? checkInRepository.findByDueDateLessThanAndStatus(today, "DUE")
                : checkInRepository.findByObjectOfficeIdAndDueDateLessThanAndStatus(officeId, today, "DUE");
        return checks.stream()
                .map(c -> new DueItem(c.getId(), c.getObject().getId(),
                        mapper.maskedName(c.getObject()),
                        c.getObject().getStatus().name(), c.getObject().getStatus().getLabel(),
                        c.getObject().getCrimeType()))
                .toList();
    }
}
