package cn.gov.sfj.jiaowutong.repo;

import cn.gov.sfj.jiaowutong.domain.ViolationType;
import cn.gov.sfj.jiaowutong.model.Violation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ViolationRepository extends JpaRepository<Violation, Long> {
    long countByObjectOfficeIdAndHandledFalse(Long officeId);

    long countByHandledFalse();

    List<Violation> findTop10ByObjectOfficeIdAndHandledFalseOrderByOccurredAtDesc(Long officeId);

    List<Violation> findTop20ByHandledFalseOrderByOccurredAtDesc();

    List<Violation> findTop20ByObjectOfficeIdAndHandledFalseOrderByOccurredAtDesc(Long officeId);

    List<Violation> findByObjectIdAndHandledFalseOrderByOccurredAtDesc(Long objectId);

    long countByObjectIdAndHandledFalse(Long objectId);

    List<Violation> findByObjectIdOrderByOccurredAtDesc(Long objectId);

    boolean existsByObjectIdAndTypeAndHandledFalse(Long objectId, ViolationType type);
}
