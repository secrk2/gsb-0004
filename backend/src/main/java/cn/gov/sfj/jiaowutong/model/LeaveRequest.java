package cn.gov.sfj.jiaowutong.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "leave_request")
public class LeaveRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "object_id")
    private CorrectionObject object;

    @Column(nullable = false, length = 256)
    private String reason;

    @Column(length = 128)
    private String destination;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /** PENDING / APPROVED / REJECTED / RETURNED 已销假 */
    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "approver", length = 64)
    private String approver;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
