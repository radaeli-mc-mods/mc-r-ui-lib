package com.radaeli.ruilib.ui;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class CurveMathTest {
    /** The control points of the Create Aeronautics pressure curve reference graph. */
    private static final List<CurvePoint> PRESSURE = List.of(
            new CurvePoint(-38, 1.5, -0.006),
            new CurvePoint(63, 1, -0.004),
            new CurvePoint(280, 0.4, -0.0016),
            new CurvePoint(320, 0, -0.02));

    /** Direct transcription of the reference B(s10, s20, kx, ky, x) polynomial. */
    private static double reference(double s10, double s20, double kx, double ky, double x) {
        return ((((s20 + s10) * kx - 2 * ky) * x + (3 * ky - kx * (s20 + 2 * s10))) * x + kx * s10) * x;
    }

    @Test public void hermiteMatchesTheReferencePolynomialEverywhere() {
        for (double x = -38; x <= 320; x += 0.5) {
            int i = 0;
            while (i < PRESSURE.size() - 2 && x > PRESSURE.get(i + 1).x()) i++;
            CurvePoint a = PRESSURE.get(i);
            CurvePoint b = PRESSURE.get(i + 1);
            double expected = reference(a.slope(), b.slope(), b.x() - a.x(), b.y() - a.y(),
                    (x - a.x()) / (b.x() - a.x())) + a.y();
            assertEquals("x=" + x, expected, CurveMath.hermite(PRESSURE, x), 1e-12);
        }
    }

    @Test public void hermitePassesThroughPointsWithTheirSlopesAndHoldsTheEnds() {
        double h = 1e-5;
        for (CurvePoint point : PRESSURE) {
            assertEquals(point.y(), CurveMath.hermite(PRESSURE, point.x()), 1e-12);
            // One-sided differences on the curve side; the ends are flat beyond the outermost points.
            double left = (point.y() - CurveMath.hermite(PRESSURE, point.x() - h)) / h;
            double right = (CurveMath.hermite(PRESSURE, point.x() + h) - point.y()) / h;
            if (point != PRESSURE.get(0)) assertEquals(point.slope(), left, 1e-6);
            if (point != PRESSURE.get(PRESSURE.size() - 1)) assertEquals(point.slope(), right, 1e-6);
        }
        assertEquals(-0.004, CurveMath.derivative(CurveModel.HERMITE, PRESSURE, 63, h), 1e-6);
        assertEquals(1.5, CurveMath.hermite(PRESSURE, -1000), 0);
        assertEquals(0, CurveMath.hermite(PRESSURE, 1000), 0);
        assertEquals(1.2869, CurveMath.hermite(PRESSURE, 0), 1e-3);
        assertEquals(7, CurveMath.hermite(List.of(new CurvePoint(3, 7, 2)), 100), 0);
        assertEquals(0, CurveMath.hermite(List.of(), 1), 0);
    }

    @Test public void linearInterpolatesAndIgnoresSlopes() {
        assertEquals(1.25, CurveMath.linear(PRESSURE, 12.5), 1e-12);
        assertEquals(0.2, CurveMath.linear(PRESSURE, 300), 1e-12);
        assertEquals(1.5, CurveMath.linear(PRESSURE, -50), 0);
    }

    @Test public void segmentSearchFindsTheContainingSegment() {
        assertEquals(0, CurveMath.segmentStart(PRESSURE, -38));
        assertEquals(0, CurveMath.segmentStart(PRESSURE, 63));
        assertEquals(1, CurveMath.segmentStart(PRESSURE, 63.01));
        assertEquals(2, CurveMath.segmentStart(PRESSURE, 319));
        assertEquals(2, CurveMath.segmentStart(PRESSURE, 5000));
    }

    @Test public void editConstraintsKeepPointsOrderedAndInsideLimits() {
        assertEquals(62, CurveMath.clampX(PRESSURE, 0, 100, 1, -100, 400), 0);
        assertEquals(-100, CurveMath.clampX(PRESSURE, 0, -500, 1, -100, 400), 0);
        assertEquals(64, CurveMath.clampX(PRESSURE, 2, 10, 1, -100, 400), 0);
        assertEquals(400, CurveMath.clampX(PRESSURE, 3, 900, 1, -100, 400), 0);
        assertEquals(1, CurveMath.insertionIndex(PRESSURE, 0, 1));
        assertEquals(4, CurveMath.insertionIndex(PRESSURE, 350, 1));
        assertEquals(-1, CurveMath.insertionIndex(PRESSURE, 63.5, 1));
        assertEquals(12.5, CurveMath.snap(12.4, 2.5), 1e-12);
        assertEquals(12.4, CurveMath.snap(12.4, 0), 0);
        assertThrows(IllegalArgumentException.class,
                () -> CurveMath.sorted(List.of(new CurvePoint(1, 0), new CurvePoint(1, 2))));
        assertEquals(63, CurveMath.sorted(List.of(PRESSURE.get(1), PRESSURE.get(0))).get(1).x(), 0);
    }

    @Test public void ticksUseRoundStepsWithoutDrift() {
        assertEquals(50, CurveMath.niceStep(358, 8), 0);
        assertEquals(0.5, CurveMath.niceStep(1.9, 4), 0);
        assertEquals(List.of(-50.0, 0.0, 50.0, 100.0), CurveMath.ticks(-80, 120, 50));
        assertEquals(List.of(0.0, 0.1, 0.2, 0.3), CurveMath.ticks(0, 0.3, 0.1));
        assertEquals(1, CurveMath.decimals(0.5));
        assertEquals(0, CurveMath.decimals(50));
        assertEquals("0", CurveMath.format(-0.0001, 2));
        assertEquals("1.25", CurveMath.format(1.25, 3));
    }

    @Test public void viewportMappingRoundTripsAndConvertsSlopes() {
        CurveViewport viewport = new CurveViewport(-80, 350, -0.2, 1.9);
        Bounds plot = new Bounds(10, 20, 430, 210);
        assertEquals(10, viewport.toScreenX(-80, plot), 1e-9);
        assertEquals(230, viewport.toScreenY(-0.2, plot), 1e-9);
        assertEquals(42.5, viewport.toDataX(viewport.toScreenX(42.5, plot), plot), 1e-9);
        assertEquals(0.75, viewport.toDataY(viewport.toScreenY(0.75, plot), plot), 1e-9);
        double screenSlope = viewport.toScreenSlope(-0.004, plot);
        assertEquals(-0.004, viewport.toDataSlope(10, 10 * screenSlope, plot), 1e-12);
        assertEquals(-0.004, viewport.toDataSlope(-10, -10 * screenSlope, plot), 1e-12);
        assertThrows(IllegalArgumentException.class, () -> new CurveViewport(1, 1, 0, 1));
    }
}
