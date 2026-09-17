package cn.gov.sfj.jiaowutong.model;

import cn.gov.sfj.jiaowutong.domain.ViolationType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "violation", indexes = {
        @Index(columnList = "object_id"),
        @Index(columnList = "handled")
})
public class Violation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "object_id")
    private CorrectionObject object;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private ViolationType type;

    @Column(length = 256)
    private String detail;

    private Double lat;

    private Double lng;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    /** 未处理的违规作为作战台红点，处理后消除 */
    @Column(nullable = false)
    private boolean handled = false;

    @Column(name = "handled_by", length = 64)
    private String handledBy;

    @Column(name = "handled_note", length = 256)
    private String handledNote;

    @Column(name = "handled_at")
    private Instant handledAt;
}
