package com.swachhdrishti.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

// Matches "repeat_monitors". One row per closed complaint, watched for 72
// hours to catch a worker who faked the cleanup.
@Entity
@Table(name = "repeat_monitors")
@Getter
@Setter
@NoArgsConstructor
public class RepeatMonitor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long monitorId;

    @ManyToOne
    @JoinColumn(name = "complaint_id", nullable = false)
    private Complaint complaint;

    private Double latitude;
    private Double longitude;

    // When the cleanup was verified. Only complaints filed AFTER this moment
    // count as a repeat. (Nullable for rows created before this column existed.)
    private LocalDateTime closedAt = LocalDateTime.now();

    private LocalDateTime expiryAt;

    private Boolean triggered = false;
}
