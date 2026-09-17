package cn.gov.sfj.jiaowutong.repo;

import cn.gov.sfj.jiaowutong.model.NameViewLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NameViewLogRepository extends JpaRepository<NameViewLog, Long> {
    List<NameViewLog> findByObjectIdOrderByCreatedAtDesc(Long objectId);
}
