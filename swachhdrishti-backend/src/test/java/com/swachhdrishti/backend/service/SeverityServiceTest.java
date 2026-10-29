package com.swachhdrishti.backend.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Locks the severity / SLA tables to the IEEE paper (Section IV-A). */
class SeverityServiceTest {

    private final SeverityService service = new SeverityService();

    @Test
    void coverageMapsToFiveLevels() {
        assertEquals(1, service.calculateSeverity(0));
        assertEquals(1, service.calculateSeverity(4.99));
        assertEquals(2, service.calculateSeverity(5));
        assertEquals(2, service.calculateSeverity(14.99));
        assertEquals(3, service.calculateSeverity(15));
        assertEquals(3, service.calculateSeverity(34.99));
        assertEquals(4, service.calculateSeverity(35));
        assertEquals(4, service.calculateSeverity(59.99));
        assertEquals(5, service.calculateSeverity(60));
        assertEquals(5, service.calculateSeverity(100));
    }

    @Test
    void slaGetsShorterAsSeverityRises() {
        assertEquals(48, service.calculateSlaHours(1));
        assertEquals(24, service.calculateSlaHours(2));
        assertEquals(12, service.calculateSlaHours(3));
        assertEquals(6, service.calculateSlaHours(4));
        assertEquals(0, service.calculateSlaHours(5)); // immediate dispatch
    }

    @Test
    void immediateDispatchStillGetsAGraceWindowForDueTime() {
        assertEquals(1, SeverityService.effectiveSlaHours(0));
        assertEquals(6, SeverityService.effectiveSlaHours(6));
    }
}
