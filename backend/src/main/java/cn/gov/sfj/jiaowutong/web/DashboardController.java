package cn.gov.sfj.jiaowutong.web;

import cn.gov.sfj.jiaowutong.common.RequestUser;
import cn.gov.sfj.jiaowutong.dto.Dtos.DashboardResp;
import cn.gov.sfj.jiaowutong.service.DashboardService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public DashboardResp dashboard(HttpServletRequest request) {
        return dashboardService.dashboard(RequestUser.get(request));
    }
}
