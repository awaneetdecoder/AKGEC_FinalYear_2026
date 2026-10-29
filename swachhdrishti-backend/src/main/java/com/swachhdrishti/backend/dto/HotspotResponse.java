package com.swachhdrishti.backend.dto;

import com.swachhdrishti.backend.entity.HotspotCluster;

import java.time.LocalDateTime;

public record HotspotResponse(Double latitude, Double longitude, Integer complaintCount,
                              String riskLevel, LocalDateTime generatedAt) {

    public static HotspotResponse from(HotspotCluster h) {
        return new HotspotResponse(h.getCenterLatitude(), h.getCenterLongitude(),
                h.getComplaintCount(), h.getRiskLevel().name(), h.getGeneratedAt());
    }
}
