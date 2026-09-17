package cn.gov.sfj.jiaowutong.web;

import cn.gov.sfj.jiaowutong.common.ApiException;
import cn.gov.sfj.jiaowutong.common.RequestUser;
import cn.gov.sfj.jiaowutong.domain.UserRole;
import cn.gov.sfj.jiaowutong.model.AppUser;
import cn.gov.sfj.jiaowutong.model.Violation;
import cn.gov.sfj.jiaowutong.repo.ViolationRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/violations")
@RequiredArgsConstructor
public class ViolationController {

    private final ViolationRepository violationRepository;

    /** 处理红点：填处理意见后消除 */
    @PostMapping("/{id}/handle")
    @Transactional
    public Map<String, Object> handle(HttpServletRequest request, @PathVariable Long id,
                                      @RequestBody Map<String, String> body) {
        AppUser user = RequestUser.get(request);
        Violation v = violationRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("违规记录不存在"));
        if (user.getRole() == UserRole.OBJECT) {
            throw ApiException.forbidden("矫正对象无权处理违规记录");
        }
        if (user.getOffice() != null
                && !v.getObject().getOffice().getId().equals(user.getOffice().getId())) {
            throw ApiException.forbidden("越权操作被拦截：该违规记录不属于您所在司法所");
        }
        if (v.isHandled()) {
            throw ApiException.badRequest("该违规已处理，请勿重复提交");
        }
        String note = body == null ? "" : body.getOrDefault("note", "").trim();
        if (note.length() < 2) {
            throw ApiException.badRequest("请填写不少于2个字的处理意见");
        }
        v.setHandled(true);
        v.setHandledBy(user.getDisplayName());
        v.setHandledNote(note);
        v.setHandledAt(Instant.now());
        violationRepository.save(v);
        return Map.of("ok", true);
    }
}
