package cn.gov.sfj.jiaowutong.repo;

import cn.gov.sfj.jiaowutong.model.JudicialOffice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JudicialOfficeRepository extends JpaRepository<JudicialOffice, Long> {
    Optional<JudicialOffice> findByCode(String code);
}
