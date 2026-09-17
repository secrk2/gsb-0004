package cn.gov.sfj.jiaowutong.repo;

import cn.gov.sfj.jiaowutong.model.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    List<LeaveRequest> findByObjectIdOrderByCreatedAtDesc(Long objectId);
}
