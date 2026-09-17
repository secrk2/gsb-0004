package cn.gov.sfj.jiaowutong.model;

import cn.gov.sfj.jiaowutong.domain.ObjStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "correction_object", indexes = {
        @Index(columnList = "office_id"),
        @Index(columnList = "status")
})
public class CorrectionObject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 矫正编号，如 JWT2026-0007 */
    @Column(nullable = false, unique = true, length = 32)
    private String code;

    /** 真实姓名，仅二次确认留痕后可查看 */
    @Column(name = "full_name", nullable = false, length = 64)
    private String fullName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "office_id", nullable = false)
    private JudicialOffice office;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ObjStatus status;

    @Column(name = "crime_type", length = 128)
    private String crimeType;

    @Column(name = "sentence_start")
    private LocalDate sentenceStart;

    @Column(name = "sentence_end")
    private LocalDate sentenceEnd;

    @Column(length = 32)
    private String phone;

    @Column(length = 256)
    private String address;

    /** 法定住址/报到点，电子围栏圆心 */
    @Column(name = "home_lat")
    private Double homeLat;

    @Column(name = "home_lng")
    private Double homeLng;

    /** 电子围栏半径（米） */
    @Column(name = "fence_radius")
    private Integer fenceRadiusMeters;

    @Column(length = 64)
    private String guardian;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
