package cn.gov.sfj.jiaowutong.service;

import cn.gov.sfj.jiaowutong.domain.ObjStatus;
import cn.gov.sfj.jiaowutong.dto.Dtos.*;
import cn.gov.sfj.jiaowutong.model.CorrectionObject;
import cn.gov.sfj.jiaowutong.model.Violation;
import cn.gov.sfj.jiaowutong.repo.ViolationRepository;
import cn.gov.sfj.jiaowutong.util.NameMasker;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ObjectMapper {

    private final ViolationRepository violationRepository;

    public ObjectMapper(ViolationRepository violationRepository) {
        this.violationRepository = violationRepository;
    }

    public String maskedName(CorrectionObject obj) {
        return NameMasker.mask(obj.getFullName(), obj.getCode());
    }

    public ObjectSummaryDto toSummary(CorrectionObject obj) {
        boolean open = violationRepository.countByObjectIdAndHandledFalse(obj.getId()) > 0;
        return new ObjectSummaryDto(
                obj.getId(), obj.getCode(), maskedName(obj),
                obj.getStatus().name(), obj.getStatus().getLabel(),
                obj.getCrimeType(), obj.getOffice().getName(),
                obj.getSentenceEnd(), open);
    }

    public ObjectDetailDto toDetail(CorrectionObject obj, boolean nameRevealed) {
        long openCount = violationRepository.countByObjectIdAndHandledFalse(obj.getId());
        List<String> next = obj.getStatus() == ObjStatus.TERMINATED
                ? List.of()
                : StateMachine.nextOf(obj.getStatus()).stream().map(Enum::name).toList();
        return new ObjectDetailDto(
                obj.getId(), obj.getCode(), maskedName(obj),
                nameRevealed ? obj.getFullName() : null,
                obj.getStatus().name(), obj.getStatus().getLabel(),
                obj.getOffice().getId(), obj.getOffice().getName(),
                obj.getCrimeType(), obj.getSentenceStart(), obj.getSentenceEnd(),
                obj.getPhone(), obj.getAddress(), obj.getGuardian(),
                obj.getHomeLat(), obj.getHomeLng(), obj.getFenceRadiusMeters(),
                nameRevealed, next, openCount);
    }

    public RedDotItem toRedDot(Violation v) {
        return new RedDotItem(v.getId(), v.getObject().getId(),
                maskedName(v.getObject()), v.getType().name(), v.getType().getLabel(),
                v.getDetail(), v.getOccurredAt());
    }
}
