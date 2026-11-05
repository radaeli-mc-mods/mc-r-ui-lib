package com.radaeli.ruilib.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

/**
 * Small outlined "?" that shows an explanation on hover. The text is the component's tooltip, so the
 * toolkit's native tooltip pipeline draws it like any other tooltip.
 */
public final class InfoBox extends UiComponent {
    /** Square side in pixels: a 9 px box matches one toolkit text row. */
    public static final int SIZE = 9;
    private static final String MARK = "?";

    public InfoBox(Component... lines) {
        tooltip(lines);
    }

    public InfoBox(Tooltip tooltip) {
        tooltip(tooltip);
    }

    @Override
    protected UiSize measureIntrinsic(Font font, UiTheme theme, int maxWidth, int maxHeight) {
        return new UiSize(SIZE, SIZE).constrain(maxWidth, maxHeight);
    }

    @Override
    public void render(UiContext context) {
        Bounds bounds = bounds();
        boolean hovered = isEnabled() && bounds.contains(context.mouseX(), context.mouseY());
        var palette = context.theme().palette();
        if (hovered) UiPaint.rect(context, bounds, UiPaint.withAlpha(palette.buttonHover(), 0.6));
        UiPaint.outline(context, bounds, hovered ? palette.accent() : palette.border());
        UiPaint.text(context, MARK, bounds.centerX() - context.font().width(MARK) / 2 + 1,
                bounds.centerY() - context.font().lineHeight / 2 + 1,
                hovered ? palette.text() : palette.mutedText());
    }

    @Override
    public boolean isFocusable() {
        return false;
    }
}
