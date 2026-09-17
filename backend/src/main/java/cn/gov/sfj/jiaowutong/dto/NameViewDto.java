package cn.gov.sfj.jiaowutong.dto;

import java.time.Instant;

public record NameViewDto(Long id, Long objectId, String viewerName, String viewerRole,
                          String reason, Instant createdAt) {
}
