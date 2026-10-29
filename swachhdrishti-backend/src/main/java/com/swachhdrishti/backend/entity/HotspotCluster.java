package com.swachhdrishti.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

// Matches "hotspot_clusters". Output of the weekly K-Means job — predicted
// high-risk zones shown as a heatmap on the officer dashboard.
@Entity
@Table(name = "hotspot_clusters")
@Getter
@Setter
@NoArgsConstructor
public class HotspotCluster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long clusterId;

    private Double centerLatitude;
    private Double centerLongitude;

    private Integer complaintCount;

    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel;

    private LocalDateTime generatedAt = LocalDateTime.now();

    public enum RiskLevel {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}
