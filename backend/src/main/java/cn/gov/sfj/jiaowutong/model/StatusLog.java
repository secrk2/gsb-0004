package cn.gov.sfj.jiaowutong.model;

import cn.gov.sfj.jiaowutong.domain.ObjStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** 状态机流转留痕 */
@Getter
@Setter
@Entity
@Table(name = "status_log", indexes = @Index(columnList = "object_id"))
public class StatusLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "object_id")
    private CorrectionObject object;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 16)
    private ObjStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 16)
    private ObjStatus toStatus;

    @Column(name = "operator_name", nullable = false, length = 64)
    private String operatorName;

    @Column(length = 256)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
