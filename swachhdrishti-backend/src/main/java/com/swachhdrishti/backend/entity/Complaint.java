package com.swachhdrishti.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

// Matches the "complaints" table. This is the core record: one garbage report.
@Entity
@Table(name = "complaints", indexes = {
        @Index(name = "idx_complaints_status", columnList = "status"),
        @Index(name = "idx_complaints_location", columnList = "latitude,longitude")
})
@Getter
@Setter
@NoArgsConstructor
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long complaintId;

    @ManyToOne
    @JoinColumn(name = "citizen_id", nullable = false)
    private User citizen;

    @Column(length = 500, nullable = false)
    private String photoUrl;

    private Double latitude;
    private Double longitude;

    private String garbageType;

    private Double boundingBoxAreaPct;

    private Integer severity;

    private Integer slaDeadlineHours;

    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDING;

    @ManyToOne
    @JoinColumn(name = "assigned_worker_id")
    private User assignedWorker;

    // True when the AI could not give a confident reading (timeout, error or
    // every detection below the confidence threshold). The complaint was given
    // the documented fallback (Mixed / severity 3) and a human should review it.
    private Boolean needsReview = false;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime cleanedAt;

    public enum Status {
        PENDING, ASSIGNED, CLEANED, REOPENED, CLOSED
    }
}
