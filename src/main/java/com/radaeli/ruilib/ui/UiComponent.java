package com.radaeli.ruilib.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/** Base node for measurement, layout, rendering and input in a toolkit tree. */
public abstract class UiComponent {
    private Bounds bounds = Bounds.EMPTY;
    private boolean focused;
    private boolean enabled = true;
    private Integer preferredWidth;
    private Integer preferredHeight;
    private Tooltip tooltip;
    private final Map<String, TransitionState> transitions = new HashMap<>();

    /** Interpolates a visual property using the shared duration from the active theme. */
    protected final double animate(UiContext context, String property, double target) {
        return animateFrom(context, property, target, target);
    }

    /** Like {@link #animate(UiContext, String, double)}, but selects the first-frame visual value. */
    protected final double animateFrom(UiContext context, String property, double target, double initial) {
        long now = context.frameTimeNanos();
        UiMotion motion = context.theme().tokens().motion();
        int durationMillis = motion.transitionDurationMillis();
        TransitionState state = transitions.computeIfAbsent(property,
                ignored -> new TransitionState(initial, now));
        double current = state.valueAt(now, durationMillis, motion.easing());
        if (Double.compare(target, state.target) != 0) {
            state.from = current;
            state.target = target;
            state.startedAt = now;
        }
        return state.valueAt(now, durationMillis, motion.easing());
    }

    protected final void clearAnimation(String property) {
        transitions.remove(property);
    }

    private static final class TransitionState {
        private double from;
        private double target;
        private long startedAt;

        private TransitionState(double initial, long now) {
            from = initial;
            target = initial;
            startedAt = now;
        }

        private double valueAt(long now, int durationMillis, UiEasing easing) {
            if (durationMillis == 0) return target;
            double durationNanos = durationMillis * 1_000_000.0;
            double progress = Math.max(0, Math.min(1, (now - startedAt) / durationNanos));
            double eased = easing.apply(progress);
            return from + (target - from) * eased;
        }
    }

    public final UiSize measure(Font font, int maxWidth, int maxHeight) {
        return measure(font, UiThemes.globalTheme(), maxWidth, maxHeight);
    }

    public final UiSize measure(Font font, UiTheme theme, int maxWidth, int maxHeight) {
        UiSize intrinsic = measureIntrinsic(font, theme, maxWidth, maxHeight);
        int width = preferredWidth == null ? intrinsic.width() : preferredWidth;
        int height = preferredHeight == null ? intrinsic.height() : preferredHeight;
        return new UiSize(width, height).constrain(maxWidth, maxHeight);
    }

    /** Measures natural content without applying this component's preferred outer size. */
    public final UiSize measureNatural(Font font, UiTheme theme, int maxWidth, int maxHeight) {
        return measureIntrinsic(font, theme, maxWidth, maxHeight).constrain(maxWidth, maxHeight);
    }

    public final UiSize measureNatural(Font font, int maxWidth, int maxHeight) {
        return measureNatural(font, UiThemes.globalTheme(), maxWidth, maxHeight);
    }

    protected abstract UiSize measureIntrinsic(Font font, UiTheme theme, int maxWidth, int maxHeight);

    /** Sets optional exact preferred dimensions; null keeps the intrinsic measurement for that axis. */
    public void setPreferredSize(Integer width, Integer height) {
        if ((width != null && width < 0) || (height != null && height < 0)) {
            throw new IllegalArgumentException("Preferred dimensions cannot be negative");
        }
        preferredWidth = width;
        preferredHeight = height;
    }

    public Integer preferredWidth() {
        return preferredWidth;
    }

    public Integer preferredHeight() {
        return preferredHeight;
    }

    public final void layout(Font font, UiTheme theme, Bounds bounds) {
        this.bounds = bounds;
        layoutChildren(font, theme);
    }

    public final void layout(Font font, Bounds bounds) {
        layout(font, UiThemes.globalTheme(), bounds);
    }

    protected void layoutChildren(Font font, UiTheme theme) {
    }

    public void render(UiContext context) {
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    public boolean charTyped(char codePoint, int modifiers) {
        return false;
    }

    public UiComponent hitTest(double mouseX, double mouseY) {
        return enabled && bounds.contains(mouseX, mouseY) ? this : null;
    }

    public void visitFocusable(java.util.function.Consumer<UiComponent> visitor) {
        if (isFocusable() && enabled) {
            visitor.accept(this);
        }
    }

    /** Visits this component and any components it contains, in render order. */
    public void visitComponents(Consumer<UiComponent> visitor) {
        Objects.requireNonNull(visitor, "visitor").accept(this);
    }

    public boolean isFocusable() {
        return false;
    }

    public boolean isFocused() {
        return focused;
    }

    public void setFocused(boolean focused) {
        this.focused = focused && isFocusable() && enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            focused = false;
        }
    }

    public Bounds bounds() {
        return bounds;
    }

    /** Attaches vanilla-rendered tooltip content to this component. */
    public UiComponent tooltip(Component... lines) {
        this.tooltip = Tooltip.of(lines);
        return this;
    }

    public UiComponent tooltip(Tooltip tooltip) {
        this.tooltip = java.util.Objects.requireNonNull(tooltip, "tooltip");
        return this;
    }

    public Tooltip tooltip() {
        return tooltip;
    }

    /** Tooltip for the mouse position; components override it to show tooltips on part of their area. */
    public Tooltip tooltipAt(double mouseX, double mouseY) {
        return tooltip();
    }
}
