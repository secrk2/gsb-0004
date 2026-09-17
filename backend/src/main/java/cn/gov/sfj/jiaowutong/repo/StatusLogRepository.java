package cn.gov.sfj.jiaowutong.repo;

import cn.gov.sfj.jiaowutong.model.StatusLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StatusLogRepository extends JpaRepository<StatusLog, Long> {
    List<StatusLog> findByObjectIdOrderByCreatedAtDesc(Long objectId);
}
