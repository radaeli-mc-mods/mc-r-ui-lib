package com.radaeli.ruilib.ui;

/** One editable control point of a {@link CurveChart}: a position and the curve's slope through it. */
public record CurvePoint(double x, double y, double slope) {
    public CurvePoint {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(slope)) {
            throw new IllegalArgumentException("Curve point coordinates and slope must be finite");
        }
    }

    public CurvePoint(double x, double y) {
        this(x, y, 0);
    }

    public CurvePoint withPosition(double x, double y) {
        return new CurvePoint(x, y, slope);
    }

    public CurvePoint withSlope(double slope) {
        return new CurvePoint(x, y, slope);
    }
}
