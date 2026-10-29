package com.swachhdrishti.backend.dto;

import com.swachhdrishti.backend.entity.ChronicZone;

import java.time.LocalDateTime;

public record ChronicZoneResponse(Long chronicZoneId, Double latitude, Double longitude,
                                  Integer repeatCount, LocalDateTime flaggedAt) {

    public static ChronicZoneResponse from(ChronicZone z) {
        return new ChronicZoneResponse(z.getChronicZoneId(), z.getLatitude(), z.getLongitude(),
                z.getRepeatCount(), z.getFlaggedAt());
    }
}
