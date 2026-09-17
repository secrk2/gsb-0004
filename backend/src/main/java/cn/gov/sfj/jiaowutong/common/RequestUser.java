package cn.gov.sfj.jiaowutong.common;

import cn.gov.sfj.jiaowutong.model.AppUser;
import jakarta.servlet.http.HttpServletRequest;

public class RequestUser {

    public static AppUser get(HttpServletRequest request) {
        Object user = request.getAttribute("user");
        if (user instanceof AppUser appUser) {
            return appUser;
        }
        throw ApiException.unauthorized("登录状态已失效，请重新登录");
    }
}
