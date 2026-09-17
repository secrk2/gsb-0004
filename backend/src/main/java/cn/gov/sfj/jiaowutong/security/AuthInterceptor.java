package cn.gov.sfj.jiaowutong.security;

import cn.gov.sfj.jiaowutong.common.ApiException;
import cn.gov.sfj.jiaowutong.model.AppUser;
import cn.gov.sfj.jiaowutong.repo.LoginTokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final LoginTokenRepository tokenRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            throw ApiException.unauthorized("缺少登录凭证，请重新登录");
        }
        String token = auth.substring("Bearer ".length()).trim();
        AppUser user = tokenRepository.findById(token)
                .orElseThrow(() -> ApiException.unauthorized("登录已失效或在别处被登出，请重新登录"))
                .getUser();
        request.setAttribute("user", user);
        return true;
    }
}
