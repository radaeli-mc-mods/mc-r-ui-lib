package com.radaeli.ruilib.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * One-line value picker: muted label on the left, the value against the right edge between
 * {@code ‹} and {@code ›} arrows. Arrows, the mouse wheel anywhere on the row and Left/Right (or
 * Up/Down) step the value; Shift multiplies numeric steps. The tooltip shows only over the label,
 * so it never covers the value or the arrows being clicked.
 *
 * <p>{@link #ofRange} steps an integer within bounds; {@link #ofOptions} cycles a list, optionally
 * starting from no value ({@code null}) until the player picks.</p>
 */
public final class Stepper<T> extends UiComponent {
    /** Extra pixels around each arrow that still count as a hit. */
    public static final int ARROW_SLACK = 3;
    private static final String MINUS = "‹";
    private static final String PLUS = "›";

    /** How the value moves and reads. {@code delta} is signed: -1/+1 per step, scaled by Shift. */
    public interface Model<T> {
        T step(T value, int delta);

        Component format(T value);
    }

    /** Row geometry, shared by the component and immediate-mode screens via {@link #layout}. */
    public record Layout(Bounds minus, Bounds plus, int labelRight, int valueX, int textY) {}

    private final Component label;
    private final Model<T> model;
    private final Consumer<T> onChange;
    private int shiftMultiplier = 10;
    private T value;
    private Layout layout;

    public Stepper(Component label, T value, Model<T> model, Consumer<T> onChange) {
        this.label = Objects.requireNonNull(label, "label");
        this.model = Objects.requireNonNull(model, "model");
        this.onChange = Objects.requireNonNull(onChange, "onChange");
        this.value = value;
    }

    /** Integer between {@code minimum} and {@code maximum}, moved by {@code step}; no wrap. */
    public static Stepper<Integer> ofRange(Component label, int minimum, int maximum, int step, int value,
                                           Function<Integer, Component> format, Consumer<Integer> onChange) {
        if (minimum > maximum || step <= 0) throw new IllegalArgumentException("Invalid stepper range");
        Objects.requireNonNull(format, "format");
        return new Stepper<>(label, Math.max(minimum, Math.min(maximum, value)), new Model<>() {
            @Override public Integer step(Integer current, int delta) {
                return stepRange(current, delta * step, minimum, maximum);
            }

            @Override public Component format(Integer current) { return format.apply(current); }
        }, onChange);
    }

    /**
     * Cycles {@code options} (wrapping when asked). A {@code null} value means none picked yet:
     * stepping forward picks the first option and backward the last. {@code empty} labels it.
     */
    public static <T> Stepper<T> ofOptions(Component label, List<T> options, T value, Function<T, Component> format,
                                           Component empty, boolean wrap, Consumer<T> onChange) {
        List<T> choices = List.copyOf(options);
        if (choices.isEmpty()) throw new IllegalArgumentException("A stepper needs options");
        Objects.requireNonNull(format, "format");
        Objects.requireNonNull(empty, "empty");
        return new Stepper<>(label, value, new Model<>() {
            @Override public T step(T current, int delta) {
                return choices.get(stepIndex(choices.indexOf(current), Integer.signum(delta), choices.size(), wrap));
            }

            @Override public Component format(T current) {
                return current == null ? empty : format.apply(current);
            }
        }, onChange);
    }

    /** Pure stepping of a bounded integer. */
    public static int stepRange(int value, int delta, int minimum, int maximum) {
        return (int) Math.max(minimum, Math.min(maximum, (long) value + delta));
    }

    /**
     * Pure stepping of an option index; {@code -1} (no option) goes to the first or last one.
     * Without wrap the ends hold.
     */
    public static int stepIndex(int index, int delta, int size, boolean wrap) {
        if (index < 0) return delta >= 0 ? 0 : size - 1;
        int next = index + delta;
        return wrap ? Math.floorMod(next, size) : Math.max(0, Math.min(size - 1, next));
    }

    public T value() {
        return value;
    }

    /** Updates the value without invoking the callback. */
    public void setValue(T value) {
        this.value = value;
    }

    /** How many steps one Shift step makes; 1 turns the multiplier off (options always move by one). */
    public Stepper<T> setShiftMultiplier(int multiplier) {
        if (multiplier < 1) throw new IllegalArgumentException("Multiplier must be positive");
        shiftMultiplier = multiplier;
        return this;
    }

    /** Steps like an arrow press: -1 or +1, scaled by Shift. */
    public void step(int direction) {
        if (!isEnabled() || direction == 0) return;
        int delta = Integer.signum(direction) * (Screen.hasShiftDown() ? shiftMultiplier : 1);
        T next = model.step(value, delta);
        if (Objects.equals(next, value)) return;
        value = next;
        onChange.accept(next);
    }

    @Override
    protected UiSize measureIntrinsic(Font font, UiTheme theme, int maxWidth, int maxHeight) {
        int gap = theme.tokens().smallGap();
        int arrows = UiText.width(font, MINUS + " ", theme) + UiText.width(font, " " + PLUS, theme);
        return new UiSize(UiText.width(font, label, theme) + gap * 2 + arrows
                + UiText.width(font, model.format(value), theme), UiText.height(font, theme) + 2)
                .constrain(maxWidth, maxHeight);
    }

    @Override
    public void render(UiContext context) {
        layout = layout(context.font(), context.theme(), bounds(), label, model.format(value));
        paint(context, bounds(), label, model.format(value), isEnabled(),
                animate(context, "focus", isFocused() ? 1 : 0));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isEnabled() || button != 0 || layout == null || !bounds().contains(mouseX, mouseY)) return false;
        if (hits(layout.minus(), mouseX, mouseY)) {
            setFocused(true);
            step(-1);
            return true;
        }
        if (hits(layout.plus(), mouseX, mouseY)) {
            setFocused(true);
            step(1);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!isEnabled() || scrollY == 0 || !bounds().contains(mouseX, mouseY)) return false;
        step(scrollY > 0 ? 1 : -1);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!isEnabled() || !isFocused()) return false;
        if (keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_UP) {
            step(1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_DOWN) {
            step(-1);
            return true;
        }
        return false;
    }

    @Override
    public boolean isFocusable() {
        return true;
    }

    @Override
    public Tooltip tooltipAt(double mouseX, double mouseY) {
        if (layout != null && mouseX >= layout.labelRight()) return null;
        return super.tooltipAt(mouseX, mouseY);
    }

    private static boolean hits(Bounds arrow, double mouseX, double mouseY) {
        return mouseX >= arrow.x() - ARROW_SLACK && mouseX < arrow.right() + ARROW_SLACK
                && mouseY >= arrow.y() && mouseY < arrow.bottom();
    }

    /**
     * Geometry of a stepper row: the arrows hug the value at the right edge and the label is cut to
     * half the row. Immediate-mode screens use it for their own click and tooltip areas.
     */
    public static Layout layout(Font font, UiTheme theme, Bounds bounds, Component label, Component value) {
        int textY = bounds.centerY() - UiText.height(font, theme) / 2;
        int plusWidth = UiText.width(font, PLUS, theme);
        int arrow = UiText.width(font, MINUS + " ", theme);
        int valueRight = bounds.right() - arrow;
        int valueX = valueRight - UiText.width(font, value, theme);
        int minusX = valueX - arrow;
        int labelRight = bounds.x() + Math.min(UiText.width(font, label, theme), bounds.width() / 2) + 2;
        return new Layout(new Bounds(minusX, bounds.y(), arrow, bounds.height()),
                new Bounds(bounds.right() - plusWidth, bounds.y(), plusWidth, bounds.height()), labelRight,
                valueX, textY);
    }

    /** Draws a stepper row; also for immediate-mode screens, with the same look as the component. */
    public static void paint(UiContext context, Bounds bounds, Component label, Component value, boolean enabled,
                             double focused) {
        UiPalette palette = context.theme().palette();
        Layout layout = layout(context.font(), context.theme(), bounds, label, value);
        String shown = UiText.width(context.font(), label, context.theme()) <= bounds.width() / 2 ? label.getString()
                : context.font().plainSubstrByWidth(label.getString(),
                Math.max(0, bounds.width() / 2 - context.font().width("…"))) + "…";
        UiPaint.text(context, shown, bounds.x(), layout.textY(),
                UiPaint.mixColor(enabled ? palette.mutedText() : palette.disabled(), palette.focus(), focused));
        UiPaint.text(context, value, layout.valueX(), layout.textY(), enabled ? palette.text() : palette.disabled());
        arrow(context, MINUS, layout.minus(), layout.textY(), enabled);
        arrow(context, PLUS, layout.plus(), layout.textY(), enabled);
    }

    private static void arrow(UiContext context, String glyph, Bounds bounds, int textY, boolean enabled) {
        UiPalette palette = context.theme().palette();
        boolean hovered = enabled && hits(bounds, context.mouseX(), context.mouseY());
        int color = !enabled ? palette.disabled() : hovered ? palette.text() : palette.accent();
        UiPaint.text(context, glyph, bounds.x(), textY, color);
    }
}
