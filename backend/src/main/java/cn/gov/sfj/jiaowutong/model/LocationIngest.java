package cn.gov.sfj.jiaowutong.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * 客户端定位点幂等台账：同一对象同一 clientId 只受理一次。
 * 即使该点因「位置未变更」被合并不落轨迹点，也在此登记，保证重试幂等。
 */
@Getter
@Setter
@Entity
@Table(name = "location_ingest",
        uniqueConstraints = @UniqueConstraint(name = "uk_ingest_object_client",
                columnNames = {"object_id", "client_id"}))
public class LocationIngest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "object_id")
    private CorrectionObject object;

    @Column(name = "client_id", nullable = false, length = 64)
    private String clientId;

    /** STORED 已落轨迹点 / MERGED 位置未变更合并 / LATE 时间戳异常丢弃 */
    @Column(nullable = false, length = 16)
    private String outcome;

    @Column(name = "point_id")
    private Long pointId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
