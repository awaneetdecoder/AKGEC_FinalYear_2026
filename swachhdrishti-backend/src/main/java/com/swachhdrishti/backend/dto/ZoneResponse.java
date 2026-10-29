package com.swachhdrishti.backend.dto;

import com.swachhdrishti.backend.entity.DisposalZone;

public record ZoneResponse(Long zoneId, String zoneName, Double latitude, Double longitude, Integer radiusMeters) {

    public static ZoneResponse from(DisposalZone z) {
        return new ZoneResponse(z.getZoneId(), z.getZoneName(), z.getLatitude(), z.getLongitude(), z.getRadiusMeters());
    }
}
