package com.radaeli.ruilib.ui;

/** Visible data range of a {@link CurveChart} and the mapping between data and GUI coordinates. */
public record CurveViewport(double xMin, double xMax, double yMin, double yMax) {
    public CurveViewport {
        if (!Double.isFinite(xMin) || !Double.isFinite(xMax) || !Double.isFinite(yMin) || !Double.isFinite(yMax)
                || !Double.isFinite(xMax - xMin) || !Double.isFinite(yMax - yMin)
                || xMax <= xMin || yMax <= yMin) {
            throw new IllegalArgumentException("Curve viewport requires finite min < max on both axes");
        }
    }

    public double width() {
        return xMax - xMin;
    }

    public double height() {
        return yMax - yMin;
    }

    public boolean contains(double x, double y) {
        return x >= xMin && x <= xMax && y >= yMin && y <= yMax;
    }

    public double toScreenX(double x, Bounds plot) {
        return plot.x() + (x - xMin) / width() * plot.width();
    }

    public double toScreenY(double y, Bounds plot) {
        return plot.y() + (1 - (y - yMin) / height()) * plot.height();
    }

    public double toDataX(double screenX, Bounds plot) {
        return xMin + (screenX - plot.x()) / Math.max(1, plot.width()) * width();
    }

    public double toDataY(double screenY, Bounds plot) {
        return yMin + (1 - (screenY - plot.y()) / Math.max(1, plot.height())) * height();
    }

    /** Converts a data-space slope into a GUI-space slope (y grows downwards on screen). */
    public double toScreenSlope(double slope, Bounds plot) {
        return -slope * (width() / Math.max(1, plot.width())) * (Math.max(1, plot.height()) / height());
    }

    /** Converts a GUI-space direction into a data-space slope; a vertical direction is nudged to 1 pixel wide. */
    public double toDataSlope(double screenDx, double screenDy, Bounds plot) {
        double dx = Math.abs(screenDx) < 1 ? Math.copySign(1, screenDx == 0 ? 1 : screenDx) : screenDx;
        double dataDx = dx / Math.max(1, plot.width()) * width();
        double dataDy = -screenDy / Math.max(1, plot.height()) * height();
        return dataDy / dataDx;
    }
}
