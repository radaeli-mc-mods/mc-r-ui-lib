package com.radaeli.ruilib.ui;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Pure helpers behind {@link CurveChart}: curve evaluation, axis ticks and edit constraints. */
public final class CurveMath {
    private static final int MAX_TICKS = 1_000;

    private CurveMath() {
    }

    /** Cubic Hermite evaluation that matches {@code B(s1, s2, kx, ky, t) + y0} per segment. */
    public static double hermite(List<CurvePoint> points, double x) {
        if (points.isEmpty()) return 0;
        int last = points.size() - 1;
        if (last == 0 || x <= points.get(0).x()) return points.get(0).y();
        if (x >= points.get(last).x()) return points.get(last).y();
        int index = segmentStart(points, x);
        CurvePoint start = points.get(index);
        CurvePoint end = points.get(index + 1);
        double kx = end.x() - start.x();
        double ky = end.y() - start.y();
        double t = (x - start.x()) / kx;
        double s1 = start.slope();
        double s2 = end.slope();
        return (((s2 + s1) * kx - 2 * ky) * t + (3 * ky - kx * (s2 + 2 * s1))) * t * t + kx * s1 * t + start.y();
    }

    public static double linear(List<CurvePoint> points, double x) {
        if (points.isEmpty()) return 0;
        int last = points.size() - 1;
        if (last == 0 || x <= points.get(0).x()) return points.get(0).y();
        if (x >= points.get(last).x()) return points.get(last).y();
        int index = segmentStart(points, x);
        CurvePoint start = points.get(index);
        CurvePoint end = points.get(index + 1);
        double t = (x - start.x()) / (end.x() - start.x());
        return start.y() + (end.y() - start.y()) * t;
    }

    /** Index of the point that starts the segment containing x, clamped to a valid segment. */
    public static int segmentStart(List<CurvePoint> points, double x) {
        int low = 0;
        int high = points.size() - 2;
        while (low < high) {
            int middle = (low + high + 1) >>> 1;
            if (points.get(middle).x() < x) low = middle;
            else high = middle - 1;
        }
        return Math.max(0, low);
    }

    /** Central-difference slope of a model at x, used to seed the slope of an inserted point. */
    public static double derivative(CurveModel model, List<CurvePoint> points, double x, double h) {
        double step = h > 0 && Double.isFinite(h) ? h : 1e-3;
        double slope = (model.evaluate(points, x + step) - model.evaluate(points, x - step)) / (2 * step);
        return Double.isFinite(slope) ? slope : 0;
    }

    /** Copies, validates and sorts points by x; rejects duplicate x values. */
    public static List<CurvePoint> sorted(List<CurvePoint> points) {
        List<CurvePoint> copy = new ArrayList<>(points.size());
        for (CurvePoint point : points) copy.add(java.util.Objects.requireNonNull(point, "point"));
        copy.sort(Comparator.comparingDouble(CurvePoint::x));
        for (int i = 1; i < copy.size(); i++) {
            if (copy.get(i).x() == copy.get(i - 1).x()) {
                throw new IllegalArgumentException("Curve points must have distinct x values");
            }
        }
        return copy;
    }

    /** Clamps a dragged x between its neighbours (keeping {@code minGap}) and the edit limits. */
    public static double clampX(List<CurvePoint> points, int index, double x, double minGap,
                                double min, double max) {
        double gap = Math.max(0, minGap);
        double lower = index > 0 ? Math.max(min, points.get(index - 1).x() + gap) : min;
        double upper = index < points.size() - 1 ? Math.min(max, points.get(index + 1).x() - gap) : max;
        if (lower > upper) return points.get(index).x();
        return Math.max(lower, Math.min(upper, x));
    }

    /** Index where a point at x would be inserted, or -1 when it would be closer than minGap to a neighbour. */
    public static int insertionIndex(List<CurvePoint> points, double x, double minGap) {
        int index = 0;
        while (index < points.size() && points.get(index).x() < x) index++;
        double gap = Math.max(0, minGap);
        if (index > 0 && x - points.get(index - 1).x() < Math.max(gap, Double.MIN_VALUE)) return -1;
        if (index < points.size() && points.get(index).x() - x < Math.max(gap, Double.MIN_VALUE)) return -1;
        return index;
    }

    public static double snap(double value, double step) {
        if (!(step > 0) || !Double.isFinite(step)) return value;
        double snapped = Math.round(value / step) * step;
        return Double.isFinite(snapped) ? snapped : value;
    }

    /** A 1-2-5 step that divides the range into at most {@code maxTicks} intervals. */
    public static double niceStep(double range, int maxTicks) {
        if (!(range > 0) || !Double.isFinite(range) || maxTicks < 1) return 1;
        double raw = range / maxTicks;
        double magnitude = Math.pow(10, Math.floor(Math.log10(raw)));
        double normalized = raw / magnitude;
        double nice = normalized <= 1 ? 1 : normalized <= 2 ? 2 : normalized <= 5 ? 5 : 10;
        return nice * magnitude;
    }

    /** Multiples of step inside [min, max], free of floating-point drift and negative zero. */
    public static List<Double> ticks(double min, double max, double step) {
        List<Double> ticks = new ArrayList<>();
        if (!(step > 0) || !Double.isFinite(min) || !Double.isFinite(max) || max < min) return ticks;
        long first = (long) Math.ceil(min / step - 1e-9);
        int decimals = decimals(step) + 2;
        for (long k = first; ticks.size() < MAX_TICKS; k++) {
            double value = BigDecimal.valueOf(k).multiply(BigDecimal.valueOf(step))
                    .setScale(decimals, RoundingMode.HALF_UP).doubleValue();
            if (value > max + step * 1e-9) break;
            ticks.add(value == 0 ? 0.0 : value);
        }
        return ticks;
    }

    /** Decimal places needed to print multiples of step exactly (0 to 6). */
    public static int decimals(double step) {
        if (!(step > 0) || !Double.isFinite(step)) return 0;
        return Math.max(0, Math.min(6, BigDecimal.valueOf(step).stripTrailingZeros().scale()));
    }

    public static String format(double value, int decimals) {
        if (!Double.isFinite(value)) return String.valueOf(value);
        String text = BigDecimal.valueOf(value).setScale(Math.max(0, decimals), RoundingMode.HALF_UP)
                .stripTrailingZeros().toPlainString();
        return text.equals("-0") ? "0" : text;
    }
}
