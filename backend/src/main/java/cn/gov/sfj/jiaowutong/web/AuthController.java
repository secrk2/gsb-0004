package cn.gov.sfj.jiaowutong.web;

import cn.gov.sfj.jiaowutong.common.RequestUser;
import cn.gov.sfj.jiaowutong.dto.Dtos.*;
import cn.gov.sfj.jiaowutong.model.AppUser;
import cn.gov.sfj.jiaowutong.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public LoginResp login(@RequestBody LoginReq req) {
        return authService.login(req);
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            authService.logout(auth.substring(7).trim());
        }
        return Map.of("ok", true);
    }

    @GetMapping("/me")
    public LoginResp me(HttpServletRequest request) {
        AppUser user = RequestUser.get(request);
        return authService.me(user);
    }
}
