package cn.gov.sfj.jiaowutong.repo;

import cn.gov.sfj.jiaowutong.domain.ObjStatus;
import cn.gov.sfj.jiaowutong.model.CorrectionObject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CorrectionObjectRepository extends JpaRepository<CorrectionObject, Long> {
    List<CorrectionObject> findByOfficeIdOrderByCode(Long officeId);

    long countByOfficeIdAndStatus(Long officeId, ObjStatus status);

    long countByStatus(ObjStatus status);

    Optional<CorrectionObject> findByCode(String code);
}
