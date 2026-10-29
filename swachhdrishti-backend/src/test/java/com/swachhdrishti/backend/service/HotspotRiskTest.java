package com.swachhdrishti.backend.service;

import com.swachhdrishti.backend.entity.HotspotCluster.RiskLevel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HotspotRiskTest {

    @Test
    void riskLevelsFollowClusterSize() {
        assertEquals(RiskLevel.LOW, HotspotSchedulerService.riskLevelFor(1));
        assertEquals(RiskLevel.LOW, HotspotSchedulerService.riskLevelFor(3));
        assertEquals(RiskLevel.MEDIUM, HotspotSchedulerService.riskLevelFor(4));
        assertEquals(RiskLevel.HIGH, HotspotSchedulerService.riskLevelFor(10));
        assertEquals(RiskLevel.CRITICAL, HotspotSchedulerService.riskLevelFor(20));
    }
}
