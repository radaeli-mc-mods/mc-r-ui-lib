package com.radaeli.ruilib.ui;

import net.minecraft.client.gui.Font;
import org.lwjgl.glfw.GLFW;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;

/** Horizontal input with an optional second handle for a strictly ordered selection interval. */
public final class Slider extends UiComponent {
    private double minimum;
    private double maximum;
    private double step;
    private double value;
    private Double upperValue;
    private final DoubleConsumer onChange;
    private DoubleFunction<String> valueFormatter;
    private BiConsumer<Double, Double> onRangeChange = (lower, upper) -> { };
    private int trackStartX;
    private int trackEndX;
    private boolean dragging;
    private boolean upperHandleActive;
    private Font layoutFont;
    private UiTheme layoutTheme;

    public Slider(double minimum, double maximum, double value, DoubleConsumer onChange) {
        this(minimum, maximum, value, 0, onChange);
    }

    /** A step of zero gives a continuous range; positive steps are snapped from the minimum. */
    public Slider(double minimum, double maximum, double value, double step, DoubleConsumer onChange) {
        validateRange(minimum, maximum, step);
        requireFinite(value);
        this.minimum = minimum;
        this.maximum = maximum;
        this.step = step;
        this.value = normalize(value);
        this.onChange = Objects.requireNonNull(onChange, "onChange");
    }

    public double minimum() {
        return minimum;
    }

    public double maximum() {
        return maximum;
    }

    public double step() {
        return step;
    }

    public double value() {
        return value;
    }

    /** Customizes the readout, including its reserved width, without changing the numeric value. */
    public Slider setValueFormatter(DoubleFunction<String> formatter) {
        valueFormatter = Objects.requireNonNull(formatter, "formatter");
        refreshTrackLayout();
        return this;
    }

    /** Returns the optional upper endpoint; null means this is a single-value slider. */
    public Double upperValue() {
        return upperValue;
    }

    public boolean hasUpperValue() {
        return upperValue != null;
    }

    /** Reports both endpoints after either changes. The upper endpoint is null in single mode. */
    public Slider setOnRangeChange(BiConsumer<Double, Double> onRangeChange) {
        this.onRangeChange = Objects.requireNonNull(onRangeChange, "onRangeChange");
        return this;
    }

    /** Enables range selection atomically. Snapped endpoints must satisfy lower < upper. */
    public void setValues(double lower, double upper) {
        requireFinite(lower);
        requireFinite(upper);
        double nextLower = normalize(lower);
        double nextUpper = normalize(upper);
        if (nextUpper <= nextLower) {
            throw new IllegalArgumentException("Slider upper value must be greater than its lower value after snapping");
        }
        boolean lowerChanged = Double.compare(value, nextLower) != 0;
        boolean changed = lowerChanged || !Objects.equals(upperValue, nextUpper);
        value = nextLower;
        upperValue = nextUpper;
        refreshTrackLayout();
        if (changed) notifyChange(lowerChanged);
    }

    /** Sets the upper endpoint, or disables it with null. Invalid ordered pairs are rejected. */
    public void setUpperValue(Double upper) {
        if (upper == null) {
            if (upperValue == null) return;
            upperValue = null;
            upperHandleActive = false;
            dragging = false;
            refreshTrackLayout();
            notifyChange(false);
            return;
        }
        setValues(value, upper);
    }

    public void setUpperValue(double upper) {
        setUpperValue(Double.valueOf(upper));
    }

    public void setRange(double minimum, double maximum) {
        validateRange(minimum, maximum, step);
        configure(minimum, maximum, step);
    }

    public void setStep(double step) {
        validateRange(minimum, maximum, step);
        configure(minimum, maximum, step);
    }

    public void setValue(double value) {
        requireFinite(value);
        double normalized = normalize(value);
        if (upperValue != null && normalized >= upperValue) normalized = previousValue(upperValue);
        if (Double.compare(normalized, this.value) == 0) return;
        this.value = normalized;
        notifyChange(true);
    }

    private void configure(double minimum, double maximum, double step) {
        double previousLower = value;
        Double previousUpper = upperValue;
        this.minimum = minimum;
        this.maximum = maximum;
        this.step = step;
        value = normalize(value);
        if (upperValue != null) {
            upperValue = normalize(upperValue);
            if (value >= upperValue) {
                if (upperValue > minimum) value = previousValue(upperValue);
                else upperValue = nextValue(value);
            }
        }
        refreshTrackLayout();
        boolean lowerChanged = Double.compare(previousLower, value) != 0;
        if (lowerChanged || !Objects.equals(previousUpper, upperValue)) notifyChange(lowerChanged);
    }

    private void notifyChange(boolean lowerChanged) {
        double lower = value;
        Double upper = upperValue;
        if (lowerChanged) onChange.accept(lower);
        onRangeChange.accept(lower, upper);
    }

    @Override
    protected UiSize measureIntrinsic(Font font, UiTheme theme, int maxWidth, int maxHeight) {
        return new UiSize(hasUpperValue() ? 220 : 140,
                Math.max(18, UiText.height(font, theme) + 8)).constrain(maxWidth, maxHeight);
    }

    @Override
    protected void layoutChildren(Font font, UiTheme theme) {
        layoutFont = font;
        layoutTheme = theme;
        int decimals = displayDecimals();
        String negativeFraction = decimals == 0 ? "-0" : "-0." + "0".repeat(decimals);
        int readoutWidth = Math.max(UiText.width(font, negativeFraction, theme),
                Math.max(UiText.width(font, formatForDisplay(minimum), theme),
                        UiText.width(font, formatForDisplay(maximum), theme)));
        if (hasUpperValue()) readoutWidth = readoutWidth * 2 + UiText.width(font, " – ", theme);
        trackStartX = bounds().x() + 6;
        trackEndX = Math.max(trackStartX + 1, bounds().right() - readoutWidth - 14);
    }

    @Override
    public void render(UiContext context) {
        int knobX = knobX(value);
        int upperKnobX = upperValue == null ? knobX : knobX(upperValue);
        int trackY = bounds().centerY();
        UiPalette palette = context.theme().palette();
        int trackColor = isEnabled() ? palette.divider() : palette.disabled();
        int radius = context.theme().tokens().radii().pill();
        UiPaint.roundedRect(context, new Bounds(trackStartX, trackY - 2,
                Math.max(1, trackEndX - trackStartX), 4), trackColor, radius);
        int selectionStart = hasUpperValue() ? knobX : trackStartX;
        int selectionEnd = hasUpperValue() ? upperKnobX : knobX;
        UiPaint.roundedRect(context, new Bounds(selectionStart, trackY - 2,
                Math.max(0, selectionEnd - selectionStart), 4),
                isEnabled() ? palette.accent() : palette.disabled(), radius);
        if(context.theme().skin().pixelated()) {
            context.theme().skin().surface(context,new Bounds(trackStartX-2,trackY-4,Math.max(5,trackEndX-trackStartX+4),8),SurfaceVariant.INSET,WorkshopSkin.WELL);
            UiPaint.rect(context,new Bounds(selectionStart,trackY-1,Math.max(0,selectionEnd-selectionStart),2),isEnabled()?palette.accent():palette.disabled());
        }
        // Draw the active handle last so it remains visible when handles are close together.
        if (hasUpperValue() && !upperHandleActive) drawKnob(context, upperKnobX, true);
        drawKnob(context, knobX, false);
        if (hasUpperValue() && upperHandleActive) drawKnob(context, upperKnobX, true);
        String readout = formatForDisplay(value);
        if (upperValue != null) readout += " – " + formatForDisplay(upperValue);
        UiPaint.text(context, readout, trackEndX + 8,
                trackY - UiText.height(context) / 2, isEnabled() ? palette.text() : palette.mutedText());
    }

    private void drawKnob(UiContext context, int knobX, boolean upper) {
        UiPalette palette = context.theme().palette();
        boolean active = upperHandleActive == upper;
        boolean hovered = isEnabled() && bounds().contains(context.mouseX(), context.mouseY())
                && nearestHandleIsUpper(context.mouseX()) == upper;
        int knobColor = !isEnabled() ? palette.mutedText()
                : UiPaint.mixColor(palette.text(), palette.buttonHover(), hovered || dragging && active ? 1 : 0);
        Bounds knob = new Bounds(knobX - 3, bounds().centerY() - 6, 7, 12);
        if(context.theme().skin().button(context,knob,new UiVisualState(isEnabled(),hovered?1:0,dragging&&active?1:0,0,isFocused()&&active?1:0)))return;
        UiPaint.roundedRect(context, knob, knobColor, context.theme().tokens().radii().pill());
        UiPaint.roundedOutline(context, knob, isFocused() && active ? palette.focus() : palette.border(),
                context.theme().tokens().radii().pill());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isEnabled() || button != 0 || !bounds().contains(mouseX, mouseY)
                || mouseX < trackStartX - 5 || mouseX > trackEndX + 5) return false;
        setFocused(true);
        upperHandleActive = nearestHandleIsUpper(mouseX);
        dragging = true;
        updateFromMouse(mouseX);
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!isEnabled() || !dragging || button != 0) return false;
        updateFromMouse(mouseX);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!dragging || button != 0) return false;
        dragging = false;
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!isEnabled() || scrollY == 0 || !bounds().contains(mouseX, mouseY)) return false;
        setFocused(true);
        upperHandleActive = nearestHandleIsUpper(mouseX);
        double increment = step > 0 ? step : (maximum - minimum) / 100.0;
        double notches = Math.max(1, Math.round(Math.abs(scrollY)));
        setActiveValue(activeValue() + Math.copySign(increment * notches, scrollY));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!isEnabled() || !isFocused()) return false;
        if (hasUpperValue() && keyCode == GLFW.GLFW_KEY_SPACE) {
            upperHandleActive = !upperHandleActive;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_HOME) {
            setActiveValue(minimum);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_END) {
            setActiveValue(maximum);
            return true;
        }
        double increment = step > 0 ? step : (maximum - minimum) / 100.0;
        if (keyCode == GLFW.GLFW_KEY_PAGE_UP || keyCode == GLFW.GLFW_KEY_PAGE_DOWN) increment *= 10;
        if (keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_UP
                || keyCode == GLFW.GLFW_KEY_PAGE_UP) {
            setActiveValue(activeValue() + increment);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_DOWN
                || keyCode == GLFW.GLFW_KEY_PAGE_DOWN) {
            setActiveValue(activeValue() - increment);
            return true;
        }
        return false;
    }

    private void updateFromMouse(double mouseX) {
        double ratio = Math.max(0, Math.min(1,
                (mouseX - trackStartX) / Math.max(1.0, trackEndX - trackStartX)));
        setActiveValue(minimum + ratio * (maximum - minimum));
    }

    private int knobX(double value) {
        double ratio = (value - minimum) / (maximum - minimum);
        return trackStartX + (int) Math.round((trackEndX - trackStartX) * ratio);
    }

    private boolean nearestHandleIsUpper(double mouseX) {
        if (upperValue == null) return false;
        double lowerDistance = Math.abs(mouseX - knobX(value));
        double upperDistance = Math.abs(mouseX - knobX(upperValue));
        return upperDistance == lowerDistance ? upperHandleActive : upperDistance < lowerDistance;
    }

    private double activeValue() {
        return upperHandleActive && upperValue != null ? upperValue : value;
    }

    private void setActiveValue(double value) {
        value = Math.max(minimum, Math.min(maximum, value));
        if (upperHandleActive && upperValue != null) {
            double next = normalize(value);
            if (next <= this.value) next = nextValue(this.value);
            setUpperValue(next);
        } else {
            setValue(value);
        }
    }

    private double previousValue(double upper) {
        double previous = Math.nextDown(upper);
        if (step > 0) {
            previous = normalize(previous);
            if (previous >= upper) previous = normalize(upper - step);
            if (previous >= upper) previous = Math.nextDown(upper);
        }
        return Math.max(minimum, previous);
    }

    private double nextValue(double lower) {
        double next = Math.nextUp(lower);
        if (step > 0) {
            next = normalize(next);
            if (next <= lower) next = normalize(lower + step);
            if (next <= lower) next = Math.nextUp(lower);
        }
        return Math.min(maximum, next);
    }

    private void refreshTrackLayout() {
        if (layoutFont != null) layoutChildren(layoutFont, layoutTheme);
    }

    private double normalize(double value) {
        double clamped = Math.max(minimum, Math.min(maximum, value));
        if (clamped == maximum) return maximum;
        if (step == 0) return clamped;
        double steps = (clamped - minimum) / step;
        if (!Double.isFinite(steps) || steps > Long.MAX_VALUE) return clamped;
        double snapped = minimum + Math.round(steps) * step;
        return Math.max(minimum, Math.min(maximum, snapped));
    }

    private static void validateRange(double minimum, double maximum, double step) {
        if (!Double.isFinite(minimum) || !Double.isFinite(maximum)
                || !Double.isFinite(maximum - minimum) || !Double.isFinite(step)
                || maximum <= minimum || step < 0) {
            throw new IllegalArgumentException("Slider requires finite min < max and a non-negative step");
        }
    }

    private static void requireFinite(double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Slider value must be finite");
    }

    private String formatForDisplay(double value) {
        if (valueFormatter != null) return valueFormatter.apply(value);
        return BigDecimal.valueOf(value).setScale(displayDecimals(), RoundingMode.HALF_UP)
                .stripTrailingZeros().toPlainString();
    }

    private int displayDecimals() {
        return step == 0 ? 3 : Math.max(0, Math.min(6,
                BigDecimal.valueOf(step).stripTrailingZeros().scale()));
    }

    @Override
    public boolean isFocusable() {
        return true;
    }
}
