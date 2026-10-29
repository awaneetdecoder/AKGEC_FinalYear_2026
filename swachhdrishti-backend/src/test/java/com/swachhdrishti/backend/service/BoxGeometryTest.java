package com.swachhdrishti.backend.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BoxGeometryTest {

    @Test
    void emptyIsZero() {
        assertEquals(0.0, BoxGeometry.unionArea(List.of()), 1e-9);
    }

    @Test
    void disjointBoxesAdd() {
        double a = BoxGeometry.unionArea(List.of(
                new BoxGeometry.Box(0, 0, 10, 10), new BoxGeometry.Box(20, 20, 25, 30)));
        assertEquals(100 + 50, a, 1e-9);
    }

    @Test
    void overlapIsCountedOnce() {
        // 10x10 and 10x10 shifted by 5 in x: union = 15 x 10 = 150 (not 200)
        double a = BoxGeometry.unionArea(List.of(
                new BoxGeometry.Box(0, 0, 10, 10), new BoxGeometry.Box(5, 0, 15, 10)));
        assertEquals(150, a, 1e-9);
    }

    @Test
    void containedBoxAddsNothing() {
        double a = BoxGeometry.unionArea(List.of(
                new BoxGeometry.Box(0, 0, 100, 100), new BoxGeometry.Box(10, 10, 20, 20)));
        assertEquals(10_000, a, 1e-9);
    }

    @Test
    void crossShapeIsInclusionExclusion() {
        // horizontal 30x10 + vertical 10x30 overlapping in a 10x10 square: 300 + 300 - 100
        double a = BoxGeometry.unionArea(List.of(
                new BoxGeometry.Box(0, 10, 30, 20), new BoxGeometry.Box(10, 0, 20, 30)));
        assertEquals(500, a, 1e-9);
    }
}
