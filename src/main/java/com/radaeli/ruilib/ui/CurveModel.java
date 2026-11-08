package com.radaeli.ruilib.ui;

import java.util.List;

/**
 * Turns the control points of a {@link CurveChart} into a function of x.
 *
 * <p>Points are always passed sorted by x with distinct x values. Implementations must be pure and
 * return finite values for finite input; the chart samples them once per screen pixel every frame.
 */
@FunctionalInterface
public interface CurveModel {
    /**
     * Piecewise cubic Hermite spline: every segment passes through both of its points with the slope
     * stored on each point, and the curve holds the first/last y beyond the outermost points.
     */
    CurveModel HERMITE = CurveMath::hermite;

    /** Straight segments between points; slopes are ignored. Holds the end values beyond the points. */
    CurveModel LINEAR = CurveMath::linear;

    double evaluate(List<CurvePoint> points, double x);
}
