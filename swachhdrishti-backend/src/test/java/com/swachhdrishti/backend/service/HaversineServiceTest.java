package com.swachhdrishti.backend.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HaversineServiceTest {

    private final HaversineService service = new HaversineService();

    @Test
    void samePointIsZero() {
        assertEquals(0.0, service.distanceInMeters(28.6692, 77.4538, 28.6692, 77.4538), 1e-9);
    }

    @Test
    void oneDegreeOfLatitudeIsAbout111Km() {
        double d = service.distanceInMeters(0, 0, 1, 0);
        assertEquals(111_195, d, 50);
    }

    @Test
    void knownCityPair() {
        // New Delhi -> Mumbai is roughly 1,150 km as the crow flies.
        double km = service.distanceInMeters(28.6139, 77.2090, 19.0760, 72.8777) / 1000;
        assertEquals(1150, km, 25);
    }

    @Test
    void fiftyMetreGeofenceAcceptsInsideAndRejectsOutside() {
        double lat = 28.6692, lng = 77.4538;
        double metreInDegLat = 1.0 / 111_195.0;

        assertTrue(service.isWithinRadius(lat, lng, lat + 49 * metreInDegLat, lng, 50));
        assertFalse(service.isWithinRadius(lat, lng, lat + 51 * metreInDegLat, lng, 50));
    }
}
