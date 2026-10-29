package com.swachhdrishti.backend.service;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * Small geometry helper: the area covered by a set of axis-aligned boxes,
 * counting overlapping regions only once. Needed because YOLO often returns
 * several overlapping boxes for one pile of garbage - summing their areas
 * would over-estimate the coverage (and so the severity).
 */
public final class BoxGeometry {

    private BoxGeometry() {}

    /** A box given by its corners; x1 &lt; x2 and y1 &lt; y2. */
    public record Box(double x1, double y1, double x2, double y2) {
        public double area() {
            return Math.max(0, x2 - x1) * Math.max(0, y2 - y1);
        }
    }

    /** Area of the union of all boxes (sweep over x-slabs, merge y-intervals). */
    public static double unionArea(List<Box> boxes) {
        if (boxes.isEmpty()) return 0;

        TreeSet<Double> xs = new TreeSet<>();
        for (Box b : boxes) {
            xs.add(b.x1());
            xs.add(b.x2());
        }
        List<Double> xList = new ArrayList<>(xs);

        double total = 0;
        for (int i = 0; i < xList.size() - 1; i++) {
            double left = xList.get(i);
            double right = xList.get(i + 1);
            double width = right - left;
            if (width <= 0) continue;

            // y-intervals of every box that fully spans this slab
            List<double[]> intervals = new ArrayList<>();
            for (Box b : boxes) {
                if (b.x1() <= left && b.x2() >= right && b.y2() > b.y1()) {
                    intervals.add(new double[]{b.y1(), b.y2()});
                }
            }
            intervals.sort((a, c) -> Double.compare(a[0], c[0]));

            double covered = 0;
            double curStart = Double.NaN;
            double curEnd = Double.NaN;
            for (double[] iv : intervals) {
                if (Double.isNaN(curEnd) || iv[0] > curEnd) {
                    if (!Double.isNaN(curEnd)) covered += curEnd - curStart;
                    curStart = iv[0];
                    curEnd = iv[1];
                } else {
                    curEnd = Math.max(curEnd, iv[1]);
                }
            }
            if (!Double.isNaN(curEnd)) covered += curEnd - curStart;

            total += width * covered;
        }
        return total;
    }
}
