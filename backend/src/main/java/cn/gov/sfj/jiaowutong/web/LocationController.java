package cn.gov.sfj.jiaowutong.web;

import cn.gov.sfj.jiaowutong.common.RequestUser;
import cn.gov.sfj.jiaowutong.dto.Dtos.*;
import cn.gov.sfj.jiaowutong.service.LocationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    /** 在线实时上报（单条） */
    @PostMapping("/report")
    public SyncBatchResp report(HttpServletRequest request, @RequestBody LocationReportReq req) {
        return locationService.report(RequestUser.get(request), req);
    }

    /** 断网恢复，离线队列批量补报（幂等+合并） */
    @PostMapping("/sync")
    public SyncBatchResp sync(HttpServletRequest request, @RequestBody SyncBatchReq req) {
        return locationService.sync(RequestUser.get(request), req);
    }

    @GetMapping("/objects/{objectId}/track")
    public List<LocationPointDto> track(HttpServletRequest request, @PathVariable Long objectId) {
        return locationService.track(RequestUser.get(request), objectId);
    }
}
