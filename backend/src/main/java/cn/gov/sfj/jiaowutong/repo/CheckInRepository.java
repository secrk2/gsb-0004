package cn.gov.sfj.jiaowutong.repo;

import cn.gov.sfj.jiaowutong.model.CheckIn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CheckInRepository extends JpaRepository<CheckIn, Long> {
    List<CheckIn> findByDueDateAndStatus(LocalDate dueDate, String status);

    List<CheckIn> findByObjectOfficeIdAndDueDateAndStatus(Long officeId, LocalDate dueDate, String status);

    List<CheckIn> findByObjectOfficeIdAndDueDateLessThanAndStatus(Long officeId, LocalDate dueDate, String status);

    List<CheckIn> findByDueDateLessThanAndStatus(LocalDate dueDate, String status);

    long countByObjectOfficeIdAndDueDateAndStatus(Long officeId, LocalDate dueDate, String status);

    long countByObjectOfficeIdAndDueDateLessThanAndStatus(Long officeId, LocalDate dueDate, String status);

    long countByDueDateAndStatus(LocalDate dueDate, String status);

    long countByDueDateLessThanAndStatus(LocalDate dueDate, String status);

    Optional<CheckIn> findByObjectIdAndDueDateAndStatus(Long objectId, LocalDate dueDate, String status);

    List<CheckIn> findByObjectIdAndDueDateLessThanEqualOrderByDueDateDesc(Long objectId, LocalDate date);
}
