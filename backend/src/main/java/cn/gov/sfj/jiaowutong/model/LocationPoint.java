package cn.gov.sfj.jiaowutong.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "location_point", indexes = {
        @Index(columnList = "object_id"),
        @Index(columnList = "recorded_at")
})
public class LocationPoint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "object_id")
    private CorrectionObject object;

    @Column(nullable = false)
    private Double lat;

    @Column(nullable = false)
    private Double lng;

    /** 定位精度（米） */
    private Double accuracy;

    /** 设备产生定位的时间（位置本身的时间，非服务端接收时间） */
    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Column(name = "reported_at", nullable = false)
    private Instant reportedAt = Instant.now();

    /** ONLINE 实时上报 / OFFLINE_SYNC 断网恢复补报 */
    @Column(nullable = false, length = 16)
    private String source;
}
