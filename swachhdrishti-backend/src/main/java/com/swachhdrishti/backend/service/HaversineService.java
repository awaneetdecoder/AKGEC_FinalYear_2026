package com.swachhdrishti.backend.service;

import org.springframework.stereotype.Service;

// Calculates real-world distance between two GPS points, in meters.
// This is the exact same formula used on the worker dashboard's radar —
// but THIS is the copy that actually decides accept/reject, since it
// runs on the server, where a worker can't tamper with it.
@Service
public class HaversineService {

    private static final double EARTH_RADIUS_METERS = 6371000;

    public double distanceInMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.pow(Math.sin(dLon / 2), 2);

        double c = 2 * Math.asin(Math.sqrt(a));
        return EARTH_RADIUS_METERS * c;
    }

    public boolean isWithinRadius(double lat1, double lon1, double lat2, double lon2, double radiusMeters) {
        return distanceInMeters(lat1, lon1, lat2, lon2) <= radiusMeters;
    }
}
