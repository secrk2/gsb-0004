package cn.gov.sfj.jiaowutong.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** 监管员/干警查看对象全名的留痕（二次确认+填理由） */
@Getter
@Setter
@Entity
@Table(name = "name_view_log", indexes = @Index(columnList = "object_id"))
public class NameViewLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "object_id")
    private CorrectionObject object;

    @Column(name = "viewer_name", nullable = false, length = 64)
    private String viewerName;

    @Column(name = "viewer_role", nullable = false, length = 16)
    private String viewerRole;

    @Column(nullable = false, length = 256)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
