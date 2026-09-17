package cn.gov.sfj.jiaowutong.web;

import cn.gov.sfj.jiaowutong.common.RequestUser;
import cn.gov.sfj.jiaowutong.dto.Dtos.*;
import cn.gov.sfj.jiaowutong.dto.NameViewDto;
import cn.gov.sfj.jiaowutong.model.CheckIn;
import cn.gov.sfj.jiaowutong.service.ObjectService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ObjectController {

    private final ObjectService objectService;

    @GetMapping("/objects")
    public List<ObjectSummaryDto> list(HttpServletRequest request) {
        return objectService.list(RequestUser.get(request));
    }

    @GetMapping("/objects/{id}")
    public ObjectDetailDto detail(HttpServletRequest request, @PathVariable Long id) {
        return objectService.detail(RequestUser.get(request), id);
    }

    @PostMapping("/objects/{id}/transitions")
    public ObjectDetailDto transit(HttpServletRequest request, @PathVariable Long id,
                                   @RequestBody StatusChangeReq req) {
        return objectService.changeStatus(RequestUser.get(request), id, req);
    }

    @PostMapping("/objects/{id}/reveal-name")
    public ObjectDetailDto revealName(HttpServletRequest request, @PathVariable Long id,
                                      @RequestBody RevealNameReq req) {
        return objectService.revealName(RequestUser.get(request), id, req);
    }

    @GetMapping("/objects/{id}/timeline")
    public List<TimelineEvent> timeline(HttpServletRequest request, @PathVariable Long id) {
        return objectService.timeline(RequestUser.get(request), id);
    }

    @GetMapping("/objects/{id}/name-view-logs")
    public List<NameViewDto> nameViewLogs(HttpServletRequest request, @PathVariable Long id) {
        return objectService.nameViewLogs(RequestUser.get(request), id).stream()
                .map(l -> new NameViewDto(l.getId(), l.getObject().getId(),
                        l.getViewerName(), l.getViewerRole(), l.getReason(), l.getCreatedAt()))
                .toList();
    }

    @GetMapping("/objects/{id}/leaves")
    public List<LeaveDto> leaves(HttpServletRequest request, @PathVariable Long id) {
        return objectService.leaves(RequestUser.get(request), id);
    }

    @PostMapping("/objects/{id}/leaves")
    public LeaveDto applyLeave(HttpServletRequest request, @PathVariable Long id,
                               @RequestBody LeaveReq req) {
        return objectService.applyLeave(RequestUser.get(request), id, req);
    }

    @PostMapping("/leaves/{id}/decision")
    public LeaveDto decide(HttpServletRequest request, @PathVariable Long id,
                           @RequestParam(defaultValue = "true") boolean approve,
                           @RequestBody(required = false) LeaveDecisionReq req) {
        return objectService.decideLeave(RequestUser.get(request), id, approve, req);
    }

    @GetMapping("/objects/{id}/check-ins")
    public List<Map<String, Object>> checkIns(HttpServletRequest request, @PathVariable Long id) {
        return objectService.checkInHistory(RequestUser.get(request), id).stream()
                .map((CheckIn c) -> Map.<String, Object>of(
                        "id", c.getId(),
                        "dueDate", c.getDueDate().toString(),
                        "status", c.getStatus(),
                        "doneAt", c.getDoneAt() == null ? "" : c.getDoneAt().toString(),
                        "method", c.getMethod() == null ? "" : c.getMethod()))
                .toList();
    }

    @PostMapping("/me/check-in")
    public Map<String, Object> checkIn(HttpServletRequest request) {
        objectService.checkIn(RequestUser.get(request));
        return Map.of("ok", true, "at", Instant.now().toString());
    }

    @PostMapping("/me/return")
    public ObjectDetailDto returnFromLeave(HttpServletRequest request) {
        return objectService.returnFromLeave(RequestUser.get(request));
    }
}
