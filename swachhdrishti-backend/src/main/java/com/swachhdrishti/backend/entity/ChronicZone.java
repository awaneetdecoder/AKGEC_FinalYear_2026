package com.swachhdrishti.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

// Matches "chronic_zones". Locations that keep having new complaints even
// after being closed — flagged automatically by the 72-hour scheduler.
@Entity
@Table(name = "chronic_zones")
@Getter
@Setter
@NoArgsConstructor
public class ChronicZone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long chronicZoneId;

    private Double latitude;
    private Double longitude;

    private Integer repeatCount = 1;

    private LocalDateTime flaggedAt = LocalDateTime.now();
}
