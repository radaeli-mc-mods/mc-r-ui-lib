package com.radaeli.ruilib.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.DoubleFunction;
import java.util.function.DoubleUnaryOperator;

/**
 * Plots a function defined by control points and a {@link CurveModel}, with axes, grid and optional
 * direct editing: drag points, drag the selected point's tangent handles to change its slope,
 * double-click the plot to add a point and right-click a point (or press Delete) to remove it.
 *
 * <p>Curves and points are rasterised at the window's real pixel density, so they stay smooth at
 * any GUI scale while the frame, grid and labels keep the toolkit's GUI-pixel look.
 */
public final class CurveChart extends UiComponent {
    private static final double POINT_RADIUS = 3;
    private static final double HALO_RADIUS = 7;
    private static final double HANDLE_RADIUS = 2.5;
    private static final double HANDLE_LENGTH = 18;
    private static final double HIT_RADIUS = 6;
    private static final double CURVE_THICKNESS = 1.25;
    private static final long DOUBLE_CLICK_NANOS = 350_000_000L;

    private List<CurvePoint> points;
    private CurveModel model = CurveModel.HERMITE;
    private CurveViewport viewport;
    private CurveViewport limits;
    private boolean editable = true;
    private boolean slopeHandles = true;
    private boolean addRemove = true;
    private boolean lockX;
    private boolean lockY;
    private int minimumPoints = 2;
    private int maximumPoints = 32;
    private double minimumGap;
    private double snapX;
    private double snapY;
    private double slopeMin = Double.NEGATIVE_INFINITY;
    private double slopeMax = Double.POSITIVE_INFINITY;
    private DoubleFunction<String> xFormatter;
    private DoubleFunction<String> yFormatter;
    private Component xLabel;
    private Component yLabel;
    private Integer curveColor;
    private Integer selectedPointColor;
    private Component xTooltipLabel;
    private Component yTooltipLabel;
    private Component slopeTooltipLabel;
    private final List<ReferenceCurve> referenceCurves = new ArrayList<>();
    private Consumer<List<CurvePoint>> onChange = ignored -> { };
    private Consumer<List<CurvePoint>> onCommit = ignored -> { };

    private int selected = -1;
    private DragTarget dragTarget = DragTarget.NONE;
    private double grabOffsetX;
    private double grabOffsetY;
    private boolean changedDuringDrag;
    private long lastClickNanos;
    private double lastClickX;
    private double lastClickY;

    private Bounds plot = Bounds.EMPTY;
    private int headerHeight;
    private List<Double> xTicks = List.of();
    private List<Double> yTicks = List.of();
    private int xDecimals;
    private int yDecimals;

    private enum DragTarget { NONE, POINT, HANDLE }

    private record ReferenceCurve(DoubleUnaryOperator function, Integer color) { }

    public CurveChart(List<CurvePoint> points, CurveViewport viewport) {
        setPoints(points);
        this.viewport = Objects.requireNonNull(viewport, "viewport");
    }

    /** Creates a chart whose viewport frames the points with a small margin. */
    public CurveChart(List<CurvePoint> points) {
        setPoints(points);
        this.viewport = framing(this.points);
    }

    // ---------------------------------------------------------------- data

    public List<CurvePoint> points() {
        return List.copyOf(points);
    }

    /** Replaces the points without firing callbacks; they are sorted by x and must have distinct x values. */
    public CurveChart setPoints(List<CurvePoint> points) {
        List<CurvePoint> sorted = CurveMath.sorted(Objects.requireNonNull(points, "points"));
        if (sorted.isEmpty()) throw new IllegalArgumentException("Curve chart requires at least one point");
        this.points = sorted;
        if (selected >= sorted.size()) selected = -1;
        dragTarget = DragTarget.NONE;
        return this;
    }

    public CurveModel model() {
        return model;
    }

    public CurveChart setModel(CurveModel model) {
        this.model = Objects.requireNonNull(model, "model");
        return this;
    }

    /** Value of the curve at x, using the current points and model. */
    public double valueAt(double x) {
        return model.evaluate(points, x);
    }

    public CurveViewport viewport() {
        return viewport;
    }

    public CurveChart setViewport(CurveViewport viewport) {
        this.viewport = Objects.requireNonNull(viewport, "viewport");
        relayout();
        return this;
    }

    public CurveChart setViewport(double xMin, double xMax, double yMin, double yMax) {
        return setViewport(new CurveViewport(xMin, xMax, yMin, yMax));
    }

    /** Range points can be dragged within; defaults to the viewport. */
    public CurveViewport limits() {
        return limits == null ? viewport : limits;
    }

    public CurveChart setLimits(CurveViewport limits) {
        this.limits = limits;
        return this;
    }

    public CurveChart setLimits(double xMin, double xMax, double yMin, double yMax) {
        return setLimits(new CurveViewport(xMin, xMax, yMin, yMax));
    }

    /** Adds a read-only function drawn behind the editable curve, e.g. a default to compare against. */
    public CurveChart addReferenceCurve(DoubleUnaryOperator function, Integer color) {
        referenceCurves.add(new ReferenceCurve(Objects.requireNonNull(function, "function"), color));
        return this;
    }

    public CurveChart clearReferenceCurves() {
        referenceCurves.clear();
        return this;
    }

    // ---------------------------------------------------------------- editing options

    public boolean isEditable() {
        return editable;
    }

    public CurveChart setEditable(boolean editable) {
        this.editable = editable;
        if (!editable) dragTarget = DragTarget.NONE;
        return this;
    }

    public CurveChart setSlopeHandles(boolean slopeHandles) {
        this.slopeHandles = slopeHandles;
        return this;
    }

    /** Allows double-click to add points and right-click/Delete to remove them. */
    public CurveChart setAllowAddRemove(boolean addRemove) {
        this.addRemove = addRemove;
        return this;
    }

    /** Locks dragging on one or both axes; slopes stay editable through the handles. */
    public CurveChart setAxisLock(boolean lockX, boolean lockY) {
        this.lockX = lockX;
        this.lockY = lockY;
        return this;
    }

    public CurveChart setPointCountRange(int minimum, int maximum) {
        if (minimum < 1 || maximum < minimum) {
            throw new IllegalArgumentException("Curve chart point range requires 1 <= minimum <= maximum");
        }
        this.minimumPoints = minimum;
        this.maximumPoints = maximum;
        return this;
    }

    /** Smallest x distance kept between neighbouring points; 0 uses 0.1% of the limits' width. */
    public CurveChart setMinimumGap(double minimumGap) {
        if (!Double.isFinite(minimumGap) || minimumGap < 0) {
            throw new IllegalArgumentException("Curve chart minimum gap must be finite and non-negative");
        }
        this.minimumGap = minimumGap;
        return this;
    }

    /** Snaps dragged coordinates to multiples of the steps; 0 disables snapping on that axis. */
    public CurveChart setSnap(double xStep, double yStep) {
        if (!Double.isFinite(xStep) || !Double.isFinite(yStep) || xStep < 0 || yStep < 0) {
            throw new IllegalArgumentException("Curve chart snap steps must be finite and non-negative");
        }
        this.snapX = xStep;
        this.snapY = yStep;
        return this;
    }

    public CurveChart setSlopeRange(double minimum, double maximum) {
        if (Double.isNaN(minimum) || Double.isNaN(maximum) || maximum < minimum) {
            throw new IllegalArgumentException("Curve chart slope range requires minimum <= maximum");
        }
        this.slopeMin = minimum;
        this.slopeMax = maximum;
        return this;
    }

    /** Called with the full point list on every edit, including each frame of a drag. */
    public CurveChart setOnChange(Consumer<List<CurvePoint>> onChange) {
        this.onChange = Objects.requireNonNull(onChange, "onChange");
        return this;
    }

    /** Called once an edit is finished: a drag released, a point added or removed, or a key nudge. */
    public CurveChart setOnCommit(Consumer<List<CurvePoint>> onCommit) {
        this.onCommit = Objects.requireNonNull(onCommit, "onCommit");
        return this;
    }

    // ---------------------------------------------------------------- appearance

    public CurveChart setAxisLabels(Component xLabel, Component yLabel) {
        this.xLabel = xLabel;
        this.yLabel = yLabel;
        relayout();
        return this;
    }

    /** Custom tick and tooltip text per axis; null restores automatic number formatting. */
    public CurveChart setFormatters(DoubleFunction<String> xFormatter, DoubleFunction<String> yFormatter) {
        this.xFormatter = xFormatter;
        this.yFormatter = yFormatter;
        relayout();
        return this;
    }

    public CurveChart setCurveColor(Integer curveColor) {
        this.curveColor = curveColor;
        return this;
    }

    /** Fill of the selected point while it is being edited; null uses the theme's text color. */
    public CurveChart setSelectedPointColor(Integer selectedPointColor) {
        this.selectedPointColor = selectedPointColor;
        return this;
    }

    /**
     * Names shown before each value in the hover tooltip, e.g. "Altitude" and "Pressure". A null
     * x or y label falls back to that axis label, then to "x"/"y"; a null slope label shows "dy/dx".
     */
    public CurveChart setTooltipLabels(Component xLabel, Component yLabel, Component slopeLabel) {
        this.xTooltipLabel = xLabel;
        this.yTooltipLabel = yLabel;
        this.slopeTooltipLabel = slopeLabel;
        return this;
    }

    public int selectedIndex() {
        return selected;
    }

    public CurveChart setSelectedIndex(int index) {
        selected = index >= 0 && index < points.size() ? index : -1;
        return this;
    }

    /** The plotting area inside the axes, in GUI coordinates; empty before layout. */
    public Bounds plotBounds() {
        return plot;
    }

    // ---------------------------------------------------------------- layout

    private Font layoutFont;
    private UiTheme layoutTheme;

    private void relayout() {
        if (layoutFont != null) layoutChildren(layoutFont, layoutTheme);
    }

    @Override
    protected UiSize measureIntrinsic(Font font, UiTheme theme, int maxWidth, int maxHeight) {
        return new UiSize(240, 150).constrain(maxWidth, maxHeight);
    }

    @Override
    protected void layoutChildren(Font font, UiTheme theme) {
        layoutFont = font;
        layoutTheme = theme;
        Bounds bounds = bounds();
        int textHeight = UiText.height(font, theme);
        headerHeight = xLabel != null || yLabel != null ? textHeight + 3 : 0;
        int top = bounds.y() + headerHeight + textHeight / 2 + 1;
        int bottom = bounds.bottom() - textHeight - 4;
        int plotHeight = Math.max(0, bottom - top);

        double yStep = CurveMath.niceStep(viewport.height(), Math.max(2, plotHeight / 26));
        yDecimals = CurveMath.decimals(yStep);
        yTicks = CurveMath.ticks(viewport.yMin(), viewport.yMax(), yStep);
        int yLabelWidth = 0;
        for (double tick : yTicks) yLabelWidth = Math.max(yLabelWidth,
                UiText.width(font, formatY(tick, yDecimals), theme));

        int widestX = Math.max(UiText.width(font, formatX(viewport.xMin(), 0), theme),
                UiText.width(font, formatX(viewport.xMax(), 0), theme));
        int left = bounds.x() + yLabelWidth + 5;
        int right = bounds.right() - Math.max(3, widestX / 2 + 1);
        int plotWidth = Math.max(0, right - left);
        double xStep = CurveMath.niceStep(viewport.width(), Math.max(2, plotWidth / Math.max(24, widestX + 14)));
        xDecimals = CurveMath.decimals(xStep);
        xTicks = CurveMath.ticks(viewport.xMin(), viewport.xMax(), xStep);
        plot = new Bounds(left, top, plotWidth, plotHeight);
    }

    private String formatX(double value, int decimals) {
        return xFormatter != null ? xFormatter.apply(value) : CurveMath.format(value, decimals);
    }

    private String formatY(double value, int decimals) {
        return yFormatter != null ? yFormatter.apply(value) : CurveMath.format(value, decimals);
    }

    // ---------------------------------------------------------------- rendering

    @Override
    public void render(UiContext context) {
        if (plot.width() <= 1 || plot.height() <= 1) return;
        UiPalette palette = context.theme().palette();
        int textHeight = UiText.height(context);
        int labelColor = palette.mutedText();

        if (yLabel != null) UiPaint.text(context, yLabel, plot.x(), bounds().y(), labelColor);
        if (xLabel != null) UiPaint.text(context, xLabel,
                plot.right() - UiText.width(context, xLabel), bounds().y(), labelColor);

        UiPaint.rect(context, plot, palette.card());
        int grid = UiPaint.mixColor(palette.card(), palette.divider(), 0.7);
        int axis = UiPaint.mixColor(palette.card(), palette.mutedText(), 0.75);
        for (double tick : xTicks) {
            int x = (int) Math.round(viewport.toScreenX(tick, plot));
            if (x < plot.x() || x >= plot.right()) continue;
            UiPaint.rect(context, new Bounds(x, plot.y(), 1, plot.height()), tick == 0 ? axis : grid);
            String label = formatX(tick, xDecimals);
            UiPaint.textCentered(context, label, x, plot.bottom() + 3, labelColor);
        }
        for (double tick : yTicks) {
            int y = (int) Math.round(viewport.toScreenY(tick, plot));
            if (y < plot.y() || y >= plot.bottom()) continue;
            UiPaint.rect(context, new Bounds(plot.x(), y, plot.width(), 1), tick == 0 ? axis : grid);
            String label = formatY(tick, yDecimals);
            UiPaint.text(context, label, plot.x() - 4 - UiText.width(context, label),
                    y - textHeight / 2, labelColor);
        }
        UiPaint.outline(context, plot, palette.border());

        Raster raster = new Raster(context, plot);
        // Batch the many small fills into one draw instead of flushing per span.
        context.graphics().drawManaged(() -> {
            raster.begin();
            try {
                for (ReferenceCurve reference : referenceCurves) {
                    int color = reference.color() != null ? reference.color()
                            : UiPaint.mixColor(palette.card(), palette.mutedText(), 0.55);
                    raster.curve(reference.function(), color, CURVE_THICKNESS);
                }
                int color = !isEnabled() ? palette.mutedText()
                        : curveColor != null ? curveColor : palette.accent();
                raster.curve(x -> model.evaluate(points, x), color, CURVE_THICKNESS * 1.2);
                renderPoints(context, raster, color);
            } finally {
                raster.end();
            }
        });
    }

    private void renderPoints(UiContext context, Raster raster, int curve) {
        UiPalette palette = context.theme().palette();
        int hovered = isEnabled() && dragTarget == DragTarget.NONE
                ? pointAt(context.mouseX(), context.mouseY()) : -1;
        for (int i = 0; i < points.size(); i++) {
            CurvePoint point = points.get(i);
            double sx = viewport.toScreenX(point.x(), plot);
            double sy = viewport.toScreenY(point.y(), plot);
            boolean active = i == hovered || i == selected || dragTarget == DragTarget.POINT && i == selected;
            double halo = animate(context, "halo:" + i, active ? 1 : 0);
            if (!plotContainsLoosely(sx, sy)) continue;
            if (halo > 0.01) {
                int haloColor = isFocused() && i == selected ? palette.focus() : curve;
                raster.disk(sx, sy, POINT_RADIUS + (HALO_RADIUS - POINT_RADIUS) * halo,
                        UiPaint.withAlpha(haloColor, 0.3 * halo));
            }
        }
        if (editable && slopeHandles && isEnabled() && selected >= 0) {
            CurvePoint point = points.get(selected);
            double sx = viewport.toScreenX(point.x(), plot);
            double sy = viewport.toScreenY(point.y(), plot);
            double[] offset = handleOffset(point);
            int lineColor = UiPaint.mixColor(palette.card(), palette.text(), 0.6);
            raster.line(sx - offset[0], sy - offset[1], sx + offset[0], sy + offset[1], lineColor, 0.75);
            for (int sign = -1; sign <= 1; sign += 2) {
                double hx = sx + offset[0] * sign;
                double hy = sy + offset[1] * sign;
                raster.disk(hx, hy, HANDLE_RADIUS, curve);
                raster.disk(hx, hy, HANDLE_RADIUS - 1, palette.card());
            }
        }
        int selectedFill = selectedPointColor != null ? selectedPointColor : palette.text();
        for (int i = 0; i < points.size(); i++) {
            CurvePoint point = points.get(i);
            double sx = viewport.toScreenX(point.x(), plot);
            double sy = viewport.toScreenY(point.y(), plot);
            if (!plotContainsLoosely(sx, sy)) continue;
            raster.disk(sx, sy, POINT_RADIUS, curve);
            // Accent ring with a contrasting centre marks the point being manipulated.
            double edit = animate(context, "edit:" + i, i == selected && editable && isEnabled() ? 1 : 0);
            if (edit > 0.01) raster.disk(sx, sy, (POINT_RADIUS - 1) * edit, selectedFill);
        }
    }

    private boolean plotContainsLoosely(double sx, double sy) {
        return sx >= plot.x() - HALO_RADIUS && sx <= plot.right() + HALO_RADIUS
                && sy >= plot.y() - HALO_RADIUS && sy <= plot.bottom() + HALO_RADIUS;
    }

    /** GUI-space offset from a point to its forward tangent handle. */
    private double[] handleOffset(CurvePoint point) {
        double screenSlope = viewport.toScreenSlope(point.slope(), plot);
        double length = Math.sqrt(1 + screenSlope * screenSlope);
        return new double[]{HANDLE_LENGTH / length, HANDLE_LENGTH * screenSlope / length};
    }

    // ---------------------------------------------------------------- hit testing

    private int pointAt(double mouseX, double mouseY) {
        int best = -1;
        double bestDistance = HIT_RADIUS * HIT_RADIUS;
        for (int i = 0; i < points.size(); i++) {
            CurvePoint point = points.get(i);
            double dx = viewport.toScreenX(point.x(), plot) - mouseX;
            double dy = viewport.toScreenY(point.y(), plot) - mouseY;
            double distance = dx * dx + dy * dy;
            if (distance <= bestDistance) {
                best = i;
                bestDistance = distance;
            }
        }
        return best;
    }

    private boolean handleAt(double mouseX, double mouseY) {
        if (!editable || !slopeHandles || selected < 0) return false;
        CurvePoint point = points.get(selected);
        double sx = viewport.toScreenX(point.x(), plot);
        double sy = viewport.toScreenY(point.y(), plot);
        double[] offset = handleOffset(point);
        for (int sign = -1; sign <= 1; sign += 2) {
            double dx = sx + offset[0] * sign - mouseX;
            double dy = sy + offset[1] * sign - mouseY;
            if (dx * dx + dy * dy <= (HANDLE_RADIUS + 3) * (HANDLE_RADIUS + 3)) return true;
        }
        return false;
    }

    @Override
    public UiComponent hitTest(double mouseX, double mouseY) {
        UiComponent hit = super.hitTest(mouseX, mouseY);
        if (hit != null || !isEnabled()) return hit;
        // Points and handles may extend slightly past the component edge.
        return pointAt(mouseX, mouseY) >= 0 || handleAt(mouseX, mouseY) ? this : null;
    }

    // ---------------------------------------------------------------- input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isEnabled() || hitTest(mouseX, mouseY) == null) return false;
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            int point = pointAt(mouseX, mouseY);
            if (point < 0 || !editable || !addRemove) return false;
            setFocused(true);
            removePoint(point);
            return true;
        }
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
        setFocused(true);
        long now = System.nanoTime();
        boolean doubleClick = now - lastClickNanos < DOUBLE_CLICK_NANOS
                && Math.abs(mouseX - lastClickX) < 4 && Math.abs(mouseY - lastClickY) < 4;
        lastClickNanos = doubleClick ? 0 : now;
        lastClickX = mouseX;
        lastClickY = mouseY;

        if (handleAt(mouseX, mouseY)) {
            dragTarget = DragTarget.HANDLE;
            changedDuringDrag = false;
            return true;
        }
        int point = pointAt(mouseX, mouseY);
        if (point >= 0) {
            selected = point;
            if (editable) {
                CurvePoint target = points.get(point);
                grabOffsetX = viewport.toScreenX(target.x(), plot) - mouseX;
                grabOffsetY = viewport.toScreenY(target.y(), plot) - mouseY;
                dragTarget = DragTarget.POINT;
                changedDuringDrag = false;
            }
            return true;
        }
        if (doubleClick && editable && addRemove && plot.contains(mouseX, mouseY)) {
            addPointAt(viewport.toDataX(mouseX, plot));
            return true;
        }
        selected = -1;
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!isEnabled() || !editable || button != GLFW.GLFW_MOUSE_BUTTON_LEFT || selected < 0) return false;
        if (dragTarget == DragTarget.POINT) {
            CurvePoint point = points.get(selected);
            CurveViewport bounds = limits();
            double x = point.x();
            double y = point.y();
            if (!lockX) {
                x = CurveMath.snap(viewport.toDataX(mouseX + grabOffsetX, plot), snapX);
                x = CurveMath.clampX(points, selected, x, effectiveGap(), bounds.xMin(), bounds.xMax());
            }
            if (!lockY) {
                y = CurveMath.snap(viewport.toDataY(mouseY + grabOffsetY, plot), snapY);
                y = Math.max(bounds.yMin(), Math.min(bounds.yMax(), y));
            }
            replaceSelected(point.withPosition(x, y));
            return true;
        }
        if (dragTarget == DragTarget.HANDLE) {
            CurvePoint point = points.get(selected);
            double dx = mouseX - viewport.toScreenX(point.x(), plot);
            double dy = mouseY - viewport.toScreenY(point.y(), plot);
            if (dx * dx + dy * dy < 4) return true;
            replaceSelected(point.withSlope(clampSlope(viewport.toDataSlope(dx, dy, plot))));
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragTarget == DragTarget.NONE || button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
        dragTarget = DragTarget.NONE;
        if (changedDuringDrag) onCommit.accept(points());
        changedDuringDrag = false;
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!isEnabled() || !isFocused()) return false;
        boolean control = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        boolean shift = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
        if (control && (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_RIGHT)) {
            int direction = keyCode == GLFW.GLFW_KEY_LEFT ? -1 : 1;
            selected = selected < 0 ? (direction > 0 ? 0 : points.size() - 1)
                    : Math.floorMod(selected + direction, points.size());
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && selected >= 0) {
            selected = -1;
            return true;
        }
        if (!editable) return false;
        if ((keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_BACKSPACE) && selected >= 0 && addRemove) {
            removePoint(selected);
            return true;
        }
        if (selected < 0) {
            if (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_RIGHT
                    || keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_DOWN) {
                selected = 0;
                return true;
            }
            return false;
        }
        double multiplier = shift ? 10 : 1;
        CurvePoint point = points.get(selected);
        CurveViewport bounds = limits();
        double xStep = (snapX > 0 ? snapX : viewport.width() / Math.max(1, plot.width())) * multiplier;
        double yStep = (snapY > 0 ? snapY : viewport.height() / Math.max(1, plot.height())) * multiplier;
        CurvePoint next = null;
        switch (keyCode) {
            case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_RIGHT -> {
                if (lockX) return true;
                double x = point.x() + (keyCode == GLFW.GLFW_KEY_LEFT ? -xStep : xStep);
                next = point.withPosition(CurveMath.clampX(points, selected, CurveMath.snap(x, snapX),
                        effectiveGap(), bounds.xMin(), bounds.xMax()), point.y());
            }
            case GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_DOWN -> {
                if (lockY) return true;
                double y = CurveMath.snap(point.y() + (keyCode == GLFW.GLFW_KEY_UP ? yStep : -yStep), snapY);
                next = point.withPosition(point.x(), Math.max(bounds.yMin(), Math.min(bounds.yMax(), y)));
            }
            case GLFW.GLFW_KEY_EQUAL, GLFW.GLFW_KEY_KP_ADD, GLFW.GLFW_KEY_MINUS, GLFW.GLFW_KEY_KP_SUBTRACT -> {
                if (!slopeHandles) return false;
                double slopeStep = viewport.height() / viewport.width() * 0.05 * multiplier;
                boolean up = keyCode == GLFW.GLFW_KEY_EQUAL || keyCode == GLFW.GLFW_KEY_KP_ADD;
                next = point.withSlope(clampSlope(point.slope() + (up ? slopeStep : -slopeStep)));
            }
            default -> {
                return false;
            }
        }
        if (!next.equals(point)) {
            replaceSelected(next);
            onCommit.accept(points());
            changedDuringDrag = false;
        }
        return true;
    }

    @Override
    public boolean isFocusable() {
        return true;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (!enabled) dragTarget = DragTarget.NONE;
    }

    @Override
    public Tooltip tooltipAt(double mouseX, double mouseY) {
        if (plot.width() <= 0) return tooltip();
        int point = dragTarget != DragTarget.NONE ? selected : pointAt(mouseX, mouseY);
        String xName = xTooltipLabel != null ? xTooltipLabel.getString() : xLabel != null ? xLabel.getString() : "x";
        String yName = yTooltipLabel != null ? yTooltipLabel.getString() : yLabel != null ? yLabel.getString() : "y";
        String slopeName = slopeTooltipLabel != null ? slopeTooltipLabel.getString() : "dy/dx";
        int xPrecision = xDecimals + 1;
        int yPrecision = yDecimals + 2;
        if (point >= 0) {
            CurvePoint value = points.get(point);
            List<Component> lines = new ArrayList<>();
            lines.add(Component.literal(xName + ": " + formatX(value.x(), xPrecision)));
            lines.add(Component.literal(yName + ": " + formatY(value.y(), yPrecision)));
            if (slopeHandles && model != CurveModel.LINEAR) {
                lines.add(Component.literal(slopeName + ": " + CurveMath.format(value.slope(), 5)));
            }
            return new Tooltip(lines);
        }
        if (plot.contains(mouseX, mouseY)) {
            double x = viewport.toDataX(mouseX, plot);
            return Tooltip.of(Component.literal(xName + ": " + formatX(x, xPrecision)),
                    Component.literal(yName + ": " + formatY(valueAt(x), yPrecision)));
        }
        return tooltip();
    }

    // ---------------------------------------------------------------- edits

    private void addPointAt(double x) {
        if (points.size() >= maximumPoints) return;
        CurveViewport bounds = limits();
        double snappedX = CurveMath.snap(x, snapX);
        if (snappedX < bounds.xMin() || snappedX > bounds.xMax()) return;
        int index = CurveMath.insertionIndex(points, snappedX, effectiveGap());
        if (index < 0) return;
        double y = Math.max(bounds.yMin(), Math.min(bounds.yMax(), valueAt(snappedX)));
        double slope = clampSlope(CurveMath.derivative(model, points, snappedX, effectiveGap() / 4));
        List<CurvePoint> next = new ArrayList<>(points);
        next.add(index, new CurvePoint(snappedX, y, slope));
        points = next;
        selected = index;
        onChange.accept(points());
        onCommit.accept(points());
    }

    private void removePoint(int index) {
        if (points.size() <= minimumPoints || index < 0 || index >= points.size()) return;
        List<CurvePoint> next = new ArrayList<>(points);
        next.remove(index);
        points = next;
        if (selected == index) selected = -1;
        else if (selected > index) selected--;
        dragTarget = DragTarget.NONE;
        onChange.accept(points());
        onCommit.accept(points());
    }

    private void replaceSelected(CurvePoint point) {
        if (point.equals(points.get(selected))) return;
        List<CurvePoint> next = new ArrayList<>(points);
        next.set(selected, point);
        points = next;
        changedDuringDrag = true;
        onChange.accept(points());
    }

    private double clampSlope(double slope) {
        if (!Double.isFinite(slope)) slope = Math.copySign(Double.MAX_VALUE, slope);
        double clamped = Math.max(slopeMin, Math.min(slopeMax, slope));
        return Double.isFinite(clamped) ? clamped : 0;
    }

    private double effectiveGap() {
        return minimumGap > 0 ? minimumGap : limits().width() * 1e-3;
    }

    private static CurveViewport framing(List<CurvePoint> points) {
        double xMin = Double.POSITIVE_INFINITY, xMax = Double.NEGATIVE_INFINITY;
        double yMin = Double.POSITIVE_INFINITY, yMax = Double.NEGATIVE_INFINITY;
        for (CurvePoint point : points) {
            xMin = Math.min(xMin, point.x());
            xMax = Math.max(xMax, point.x());
            yMin = Math.min(yMin, point.y());
            yMax = Math.max(yMax, point.y());
        }
        double width = xMax - xMin > 0 ? xMax - xMin : 1;
        double height = yMax - yMin > 0 ? yMax - yMin : 1;
        return new CurveViewport(xMin - width * 0.1, xMax + width * 0.1,
                yMin - height * 0.1, yMax + height * 0.1);
    }

    // ---------------------------------------------------------------- raster

    /** Draws in real window pixels inside a GUI-scaled frame, so curves stay smooth at any GUI scale. */
    private final class Raster {
        private final UiContext context;
        private final GuiGraphics graphics;
        private final Bounds clip;
        private final double scale;
        private final int clipLeft;
        private final int clipTop;
        private final int clipRight;
        private final int clipBottom;

        private Raster(UiContext context, Bounds clip) {
            this.context = context;
            this.graphics = context.graphics();
            this.clip = clip;
            this.scale = Math.max(1, Minecraft.getInstance().getWindow().getGuiScale());
            this.clipLeft = (int) Math.ceil(clip.x() * scale);
            this.clipTop = (int) Math.ceil(clip.y() * scale);
            this.clipRight = (int) Math.floor(clip.right() * scale);
            this.clipBottom = (int) Math.floor(clip.bottom() * scale);
        }

        private void begin() {
            graphics.pose().pushPose();
            graphics.pose().scale((float) (1 / scale), (float) (1 / scale), 1);
        }

        private void end() {
            graphics.pose().popPose();
        }

        private void fill(int x0, int y0, int x1, int y1, int color) {
            if (x1 > x0 && y1 > y0) graphics.fill(x0, y0, x1, y1, UiPaint.withAlpha(color, context.opacity()));
        }

        private void fillClipped(int x0, int y0, int x1, int y1, int color) {
            fill(Math.max(x0, clipLeft), Math.max(y0, clipTop), Math.min(x1, clipRight), Math.min(y1, clipBottom), color);
        }

        /** One span per pixel column joining consecutive samples, clipped to the plot. */
        private void curve(DoubleUnaryOperator function, int color, double thicknessGui) {
            int width = Math.max(1, (int) Math.round(thicknessGui * scale));
            double half = width / 2.0;
            int offset = width / 2;
            double previous = Double.NaN;
            for (int column = clipLeft; column < clipRight; column++) {
                double dataX = viewport.toDataX((column + 0.5) / scale, clip);
                double value;
                try {
                    value = function.applyAsDouble(dataX);
                } catch (RuntimeException ignored) {
                    value = Double.NaN;
                }
                if (!Double.isFinite(value)) {
                    previous = Double.NaN;
                    continue;
                }
                double y = viewport.toScreenY(value, clip) * scale;
                double from = Double.isNaN(previous) ? y : previous;
                int top = (int) Math.floor(Math.min(from, y) - half + 0.5);
                int bottom = (int) Math.floor(Math.max(from, y) + half + 0.5);
                fillClipped(column - offset, top, column - offset + width, Math.max(bottom, top + 1), color);
                previous = y;
            }
        }

        /** Straight segment between GUI points, stepped along its major axis; not clipped. */
        private void line(double x0, double y0, double x1, double y1, int color, double thicknessGui) {
            int width = Math.max(1, (int) Math.round(thicknessGui * scale));
            double ax = x0 * scale, ay = y0 * scale, bx = x1 * scale, by = y1 * scale;
            int steps = (int) Math.ceil(Math.max(Math.abs(bx - ax), Math.abs(by - ay)));
            for (int i = 0; i <= steps; i++) {
                double t = steps == 0 ? 0 : (double) i / steps;
                int px = (int) Math.floor(ax + (bx - ax) * t - width / 2.0 + 0.5);
                int py = (int) Math.floor(ay + (by - ay) * t - width / 2.0 + 0.5);
                fill(px, py, px + width, py + width, color);
            }
        }

        /** Filled disk centred on a GUI point; rows never overlap, so translucent colors blend evenly. */
        private void disk(double centerX, double centerY, double radiusGui, int color) {
            double radius = radiusGui * scale;
            if (radius <= 0) return;
            double cx = centerX * scale;
            double cy = centerY * scale;
            int top = (int) Math.floor(cy - radius);
            int bottom = (int) Math.ceil(cy + radius);
            for (int row = top; row < bottom; row++) {
                double dy = row + 0.5 - cy;
                double span = radius * radius - dy * dy;
                if (span <= 0) continue;
                double halfWidth = Math.sqrt(span);
                fill((int) Math.round(cx - halfWidth), row, (int) Math.round(cx + halfWidth), row + 1, color);
            }
        }
    }
}
