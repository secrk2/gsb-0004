package cn.gov.sfj.jiaowutong.service;

import cn.gov.sfj.jiaowutong.common.ApiException;
import cn.gov.sfj.jiaowutong.domain.UserRole;
import cn.gov.sfj.jiaowutong.dto.Dtos.*;
import cn.gov.sfj.jiaowutong.model.AppUser;
import cn.gov.sfj.jiaowutong.model.LoginToken;
import cn.gov.sfj.jiaowutong.repo.AppUserRepository;
import cn.gov.sfj.jiaowutong.repo.LoginTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository userRepository;
    private final LoginTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();

    @Transactional
    public LoginResp login(LoginReq req) {
        if (req == null || req.username() == null || req.password() == null) {
            throw ApiException.badRequest("请输入账号和密码");
        }
        AppUser user = userRepository.findByUsername(req.username().trim())
                .orElseThrow(() -> ApiException.unauthorized("账号或密码错误"));
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw ApiException.unauthorized("账号或密码错误");
        }
        String token = newToken();
        LoginToken loginToken = new LoginToken();
        loginToken.setToken(token);
        loginToken.setUser(user);
        tokenRepository.save(loginToken);

        Long officeId = user.getOffice() == null ? null : user.getOffice().getId();
        String officeName = user.getOffice() == null ? null : user.getOffice().getName();
        return new LoginResp(token, user.getId(), user.getDisplayName(),
                user.getRole().name(), user.getRole().getLabel(),
                officeId, officeName, user.getObjectId());
    }

    @Transactional
    public void logout(String token) {
        if (token != null) {
            tokenRepository.deleteById(token);
        }
    }

    @Transactional(readOnly = true)
    public LoginResp me(AppUser user) {
        Long officeId = user.getOffice() == null ? null : user.getOffice().getId();
        String officeName = user.getOffice() == null ? null : user.getOffice().getName();
        return new LoginResp(null, user.getId(), user.getDisplayName(),
                user.getRole().name(), user.getRole().getLabel(),
                officeId, officeName, user.getObjectId());
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        StringBuilder sb = new StringBuilder(64);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }

    public static boolean isStaff(AppUser user) {
        return user.getRole() == UserRole.SUPERVISOR || user.getRole() == UserRole.OFFICER;
    }
}
