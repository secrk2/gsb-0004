package cn.gov.sfj.jiaowutong.repo;

import cn.gov.sfj.jiaowutong.model.LoginToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginTokenRepository extends JpaRepository<LoginToken, String> {
}
