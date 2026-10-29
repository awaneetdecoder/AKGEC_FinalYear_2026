package com.swachhdrishti.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

// Matches "disposal_zones". The official, allowed dump sites for the city.
// An admin seeds this once — see DataSeeder.java for two example zones.
@Entity
@Table(name = "disposal_zones")
@Getter
@Setter
@NoArgsConstructor
public class DisposalZone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long zoneId;

    private String zoneName;

    private Double latitude;
    private Double longitude;

    private Integer radiusMeters = 50;

    private Boolean isActive = true;
}
