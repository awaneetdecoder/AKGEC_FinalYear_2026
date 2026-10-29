package com.swachhdrishti.backend.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// A from-scratch K-Means clustering implementation — no external ML
// library, exactly as your architecture describes. It groups a list of
// GPS points into K clusters and returns each cluster's center and how
// many points belong to it.
//
// This is genuinely simple math, not a black box — here's the idea in
// plain words: start with K random guesses for cluster centers, assign
// every point to whichever center is nearest, then move each center to
// the middle of the points assigned to it, and repeat until the centers
// stop moving much.
@Service
public class KMeansService {

    public record Point(double lat, double lng) {}
    public record ClusterResult(double centerLat, double centerLng, int count) {}

    public List<ClusterResult> cluster(List<Point> points, int k, int maxIterations) {
        if (points.size() < k) {
            // Not enough data yet to form k meaningful clusters — this is
            // the exact "K-Means needs 30 days of data" risk your project
            // plan already identified. Callers should treat an empty or
            // short result list as "not enough data yet", not an error.
            k = Math.max(1, points.size());
        }

        List<Point> centers = initializeCenters(points, k);

        for (int iteration = 0; iteration < maxIterations; iteration++) {
            List<List<Point>> assignments = new ArrayList<>();
            for (int i = 0; i < k; i++) assignments.add(new ArrayList<>());

            // Step 1: assign every point to its nearest current center.
            for (Point p : points) {
                int nearestIndex = nearestCenterIndex(p, centers);
                assignments.get(nearestIndex).add(p);
            }

            // Step 2: move each center to the average position of its points.
            List<Point> newCenters = new ArrayList<>();
            boolean moved = false;
            for (int i = 0; i < k; i++) {
                List<Point> group = assignments.get(i);
                if (group.isEmpty()) {
                    newCenters.add(centers.get(i)); // keep empty clusters where they were
                    continue;
                }
                double avgLat = group.stream().mapToDouble(Point::lat).average().orElse(centers.get(i).lat());
                double avgLng = group.stream().mapToDouble(Point::lng).average().orElse(centers.get(i).lng());
                Point newCenter = new Point(avgLat, avgLng);
                if (distanceSquared(newCenter, centers.get(i)) > 1e-10) moved = true;
                newCenters.add(newCenter);
            }

            centers = newCenters;
            if (!moved) break; // converged early, no need to keep iterating
        }

        // Build final result with point counts per cluster.
        List<List<Point>> finalAssignments = new ArrayList<>();
        for (int i = 0; i < k; i++) finalAssignments.add(new ArrayList<>());
        for (Point p : points) {
            finalAssignments.get(nearestCenterIndex(p, centers)).add(p);
        }

        List<ClusterResult> results = new ArrayList<>();
        for (int i = 0; i < k; i++) {
            results.add(new ClusterResult(centers.get(i).lat(), centers.get(i).lng(), finalAssignments.get(i).size()));
        }
        return results;
    }

    private List<Point> initializeCenters(List<Point> points, int k) {
        // Simple random-pick initialization. Good enough for a small demo
        // dataset; if you need more stable results later, look into
        // "K-Means++" initialization as an upgrade.
        List<Point> shuffled = new ArrayList<>(points);
        java.util.Collections.shuffle(shuffled, new Random(42)); // fixed seed = reproducible demo results
        return new ArrayList<>(shuffled.subList(0, k));
    }

    private int nearestCenterIndex(Point p, List<Point> centers) {
        int bestIndex = 0;
        double bestDist = Double.MAX_VALUE;
        for (int i = 0; i < centers.size(); i++) {
            double d = distanceSquared(p, centers.get(i));
            if (d < bestDist) {
                bestDist = d;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    private double distanceSquared(Point a, Point b) {
        double dLat = a.lat() - b.lat();
        double dLng = a.lng() - b.lng();
        return dLat * dLat + dLng * dLng;
    }
}
