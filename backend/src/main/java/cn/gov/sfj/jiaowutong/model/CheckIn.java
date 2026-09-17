package cn.gov.sfj.jiaowutong.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "check_in", indexes = {
        @Index(columnList = "object_id"),
        @Index(columnList = "due_date"),
        @Index(columnList = "status")
})
public class CheckIn {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "object_id")
    private CorrectionObject object;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    /** DUE 今日应报到 / DONE 已报到 / MISSED 逾时未报 */
    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "done_at")
    private Instant doneAt;

    @Column(length = 32)
    private String method;
}
