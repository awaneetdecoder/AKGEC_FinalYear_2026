package com.swachhdrishti.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

// Matches "disposal_logs". Records where a worker actually stood when they
// clicked "Cleaned" — this is the proof used by the GPS accountability check.
@Entity
@Table(name = "disposal_logs")
@Getter
@Setter
@NoArgsConstructor
public class DisposalLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long disposalId;

    @ManyToOne
    @JoinColumn(name = "complaint_id", nullable = false)
    private Complaint complaint;

    @ManyToOne
    @JoinColumn(name = "worker_id", nullable = false)
    private User worker;

    private Double workerLatitude;
    private Double workerLongitude;

    private Boolean isAuthorised = false;

    private LocalDateTime disposedAt = LocalDateTime.now();
}
