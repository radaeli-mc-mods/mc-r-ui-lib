package com.radaeli.ruilib.ui;

import net.minecraft.client.gui.Font;

/**
 * Non-interactive themed divider, horizontal by default. It reserves free space around the rule so
 * neighbouring content does not touch it. The spacing is an {@link Insets}: for a horizontal divider
 * only {@code top} and {@code bottom} apply; for a vertical one only {@code left} and {@code right}.
 * The default is {@link #DEFAULT_SPACING} on every side.
 */
public final class Divider extends UiComponent {
    /** Pixels kept free on each side of the rule unless a constructor says otherwise. */
    public static final int DEFAULT_SPACING = 5;

    public enum Orientation { HORIZONTAL, VERTICAL }

    private final Orientation orientation;
    private final int thickness;
    private final Integer color;
    private final Insets spacing;

    public Divider() {
        this(Orientation.HORIZONTAL, 1, null);
    }

    public Divider(Orientation orientation) {
        this(orientation, 1, null);
    }

    public Divider(Orientation orientation, int thickness, Integer color) {
        this(orientation, thickness, color, Insets.all(DEFAULT_SPACING));
    }

    /**
     * @param thickness rule width in pixels, at least 1
     * @param color     rule color, or null for the theme's divider color
     * @param spacing   free space around the rule; top and bottom for horizontal, left and right for vertical
     */
    public Divider(Orientation orientation, int thickness, Integer color, Insets spacing) {
        if (thickness < 1) throw new IllegalArgumentException("Divider thickness must be positive");
        this.orientation = java.util.Objects.requireNonNull(orientation, "orientation");
        this.thickness = thickness;
        this.color = color;
        this.spacing = java.util.Objects.requireNonNull(spacing, "spacing");
    }

    /** Free space around the rule. */
    public Insets spacing() {
        return spacing;
    }

    private int before() {
        return orientation == Orientation.HORIZONTAL ? spacing.top() : spacing.left();
    }

    private int after() {
        return orientation == Orientation.HORIZONTAL ? spacing.bottom() : spacing.right();
    }

    @Override
    protected UiSize measureIntrinsic(Font font, UiTheme theme, int maxWidth, int maxHeight) {
        int reserved = before() + thickness + after();
        return (orientation == Orientation.HORIZONTAL
                ? new UiSize(Math.min(maxWidth, 64), reserved)
                : new UiSize(reserved, Math.min(maxHeight, 64)));
    }

    @Override
    public void render(UiContext context) {
        Bounds area = bounds();
        Bounds rule = orientation == Orientation.HORIZONTAL
                ? new Bounds(area.x(), area.y() + before(), area.width(), thickness)
                : new Bounds(area.x() + before(), area.y(), thickness, area.height());
        UiPaint.rect(context, rule, color == null ? context.theme().palette().divider() : color);
    }
}
