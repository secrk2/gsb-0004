package cn.gov.sfj.jiaowutong.repo;

import cn.gov.sfj.jiaowutong.model.LocationIngest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LocationIngestRepository extends JpaRepository<LocationIngest, Long> {
    Optional<LocationIngest> findByObjectIdAndClientId(Long objectId, String clientId);
}
