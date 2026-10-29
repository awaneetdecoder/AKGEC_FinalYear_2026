package com.swachhdrishti.backend.service;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KMeansServiceTest {

    private final KMeansService service = new KMeansService();

    private static List<KMeansService.Point> blob(double lat, double lng, int n, long seed) {
        Random r = new Random(seed);
        List<KMeansService.Point> pts = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            pts.add(new KMeansService.Point(lat + (r.nextDouble() - 0.5) * 0.002, lng + (r.nextDouble() - 0.5) * 0.002));
        }
        return pts;
    }

    @Test
    void separatesTwoDistantBlobs() {
        List<KMeansService.Point> points = new ArrayList<>();
        points.addAll(blob(28.67, 77.45, 12, 1));
        points.addAll(blob(28.50, 77.20, 8, 2));

        List<KMeansService.ClusterResult> result = service.cluster(points, 2, 100);

        assertEquals(2, result.size());
        assertEquals(20, result.stream().mapToInt(KMeansService.ClusterResult::count).sum());
        assertTrue(result.stream().anyMatch(c -> c.count() == 12));
        assertTrue(result.stream().anyMatch(c -> c.count() == 8));
    }

    @Test
    void everyPointIsAssignedExactlyOnce() {
        List<KMeansService.Point> points = new ArrayList<>();
        points.addAll(blob(28.67, 77.45, 10, 3));
        points.addAll(blob(28.60, 77.30, 10, 4));
        points.addAll(blob(28.55, 77.40, 10, 5));

        List<KMeansService.ClusterResult> result = service.cluster(points, 5, 100);

        assertEquals(30, result.stream().mapToInt(KMeansService.ClusterResult::count).sum());
    }

    @Test
    void fewerPointsThanKDoesNotCrash() {
        List<KMeansService.Point> points = List.of(
                new KMeansService.Point(28.1, 77.1), new KMeansService.Point(28.2, 77.2));

        List<KMeansService.ClusterResult> result = service.cluster(points, 5, 100);

        assertEquals(2, result.size());
    }

    @Test
    void isDeterministicBecauseOfFixedSeed() {
        List<KMeansService.Point> points = blob(28.6, 77.3, 40, 9);
        assertEquals(service.cluster(points, 4, 100), service.cluster(points, 4, 100));
    }
}
