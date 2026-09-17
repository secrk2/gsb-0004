package cn.gov.sfj.jiaowutong.repo;

import cn.gov.sfj.jiaowutong.model.LocationPoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LocationPointRepository extends JpaRepository<LocationPoint, Long> {
    List<LocationPoint> findTop200ByObjectIdOrderByRecordedAtDesc(Long objectId);

    Optional<LocationPoint> findFirstByObjectIdOrderByRecordedAtDesc(Long objectId);
}
