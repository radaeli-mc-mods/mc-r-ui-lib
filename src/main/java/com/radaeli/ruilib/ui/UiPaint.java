package com.radaeli.ruilib.ui;

import net.minecraft.client.gui.GuiGraphics;
import com.radaeli.ruilib.platform.UiPlatform;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/** Small, theme-aware drawing primitives. Components should use these instead of raw shapes. */
public final class UiPaint {
    private static final float TOOLTIP_Z_INDEX = 600.0F;
    private UiPaint() {
    }

    public static void rect(UiContext context, Bounds bounds, int color) {
        if (bounds.width() > 0 && bounds.height() > 0) {
            context.graphics().fill(bounds.x(), bounds.y(), bounds.right(), bounds.bottom(),
                    withAlpha(color, context.opacity()));
        }
    }

    public static void outline(UiContext context, Bounds bounds, int color) {
        if (bounds.width() > 0 && bounds.height() > 0) {
            UiPlatform.outline(context.graphics(), bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                    withAlpha(color, context.opacity()));
        }
    }

    public static void roundedRect(UiContext context, Bounds bounds, int color, int radius) {
        if (bounds.width() <= 0 || bounds.height() <= 0) return;
        int clamped = Math.min(Math.max(0, radius), Math.min(bounds.width(), bounds.height()) / 2);
        if (clamped == 0) {
            rect(context, bounds, color);
            return;
        }
        for (int row = 0; row < bounds.height(); row++) {
            int inset = cornerInset(row, bounds.height(), clamped);
            rect(context, new Bounds(bounds.x() + inset, bounds.y() + row,
                    bounds.width() - inset * 2, 1), color);
        }
    }

    public static void roundedOutline(UiContext context, Bounds bounds, int color, int radius) {
        if (bounds.width() <= 0 || bounds.height() <= 0) return;
        int clamped = Math.min(Math.max(0, radius), Math.min(bounds.width(), bounds.height()) / 2);
        if (clamped == 0) {
            outline(context, bounds, color);
            return;
        }
        for (int row = 0; row < bounds.height(); row++) {
            int outerInset = cornerInset(row, bounds.height(), clamped);
            int left = bounds.x() + outerInset;
            int right = bounds.right() - outerInset;
            if (row == 0 || row == bounds.height() - 1) {
                rect(context, new Bounds(left, bounds.y() + row, Math.max(0, right - left), 1), color);
                continue;
            }
            int innerRow = row - 1;
            if (bounds.width() < 3) continue;
            int innerRadius = Math.max(0, clamped - 1);
            int innerInset = cornerInset(innerRow, bounds.height() - 2, innerRadius);
            int innerLeft = bounds.x() + 1 + innerInset;
            int innerRight = bounds.right() - 1 - innerInset;
            if (innerLeft > left) {
                rect(context, new Bounds(left, bounds.y() + row, innerLeft - left, 1), color);
            }
            if (innerRight < right) {
                rect(context, new Bounds(innerRight, bounds.y() + row, right - innerRight, 1), color);
            }
        }
    }

    private static int cornerInset(int row, int height, int radius) {
        int distance = row < radius ? radius - row - 1 : row >= height - radius ? row - (height - radius) : 0;
        if (distance <= 0) return 0;
        return Math.max(0, radius - (int) Math.floor(Math.sqrt(radius * radius - distance * distance)));
    }

    public static void panel(UiContext context, Bounds bounds) {
        int radius = context.theme().tokens().radii().large();
        roundedRect(context, bounds, context.theme().palette().panel(), radius);
        roundedOutline(context, bounds, context.theme().palette().border(), radius);
    }

    public static void panel(UiContext context, Bounds bounds, double opacity) {
        int radius = context.theme().tokens().radii().large();
        roundedRect(context, bounds, withAlpha(context.theme().palette().panel(), opacity), radius);
        roundedOutline(context, bounds, withAlpha(context.theme().palette().border(), opacity), radius);
    }

    /** Draws a notification with its accent clipped inside the panel's rounded border. */
    public static void toast(UiContext context, Bounds bounds, int accentColor) {
        int radius = Math.min(context.theme().tokens().radii().large(),
                Math.min(bounds.width(), bounds.height()) / 2);
        roundedRect(context, bounds, context.theme().palette().panel(), radius);
        Bounds inner = new Bounds(bounds.x() + 1, bounds.y() + 1,
                Math.max(0, bounds.width() - 2), Math.max(0, bounds.height() - 2));
        int innerRadius = Math.max(0, radius - 1);
        int accentRight = Math.min(inner.right(), inner.x() + 3);
        for (int row = 0; row < inner.height(); row++) {
            int inset = cornerInset(row, inner.height(), innerRadius);
            int left = inner.x() + inset;
            int right = Math.min(accentRight, inner.right() - inset);
            if (right > left) {
                rect(context, new Bounds(left, inner.y() + row, right - left, 1), accentColor);
            }
        }
        roundedOutline(context, bounds, context.theme().palette().border(), radius);
    }

    public static void card(UiContext context, Bounds bounds) {
        int radius = context.theme().tokens().radii().medium();
        roundedRect(context, bounds, context.theme().palette().card(), radius);
        roundedOutline(context, bounds, context.theme().palette().border(), radius);
    }

    public static void card(UiContext context, Bounds bounds, boolean hovered,
                            boolean selected, boolean focused) {
        card(context, bounds, hovered ? 1 : 0, selected ? 1 : 0, focused ? 1 : 0);
    }

    public static void card(UiContext context, Bounds bounds, double hover, double selected, double focused) {
        UiPalette palette = context.theme().palette();
        int fill = mixColor(mixColor(palette.card(), palette.panel(), hover), palette.buttonPressed(), selected);
        int radius = context.theme().tokens().radii().medium();
        roundedRect(context, bounds, fill, radius);
        int border = mixColor(mixColor(palette.border(), palette.accent(), selected), palette.focus(), focused);
        roundedOutline(context, bounds, border, radius);
    }

    public static void divider(UiContext context, Bounds bounds) {
        rect(context, bounds, context.theme().palette().divider());
    }

    public static void button(UiContext context, Bounds bounds, boolean hovered,
                              boolean pressed, boolean enabled, boolean focused) {
        button(context, bounds, hovered ? 1 : 0, pressed ? 1 : 0, enabled, focused ? 1 : 0);
    }

    public static void button(UiContext context, Bounds bounds, double hover, double pressed,
                              boolean enabled, double focused) {
        UiPalette palette = context.theme().palette();
        int fill = !enabled ? palette.disabled()
                : mixColor(mixColor(palette.button(), palette.buttonHover(), hover), palette.buttonPressed(), pressed);
        int radius = context.theme().tokens().radii().small();
        roundedRect(context, bounds, fill, radius);
        roundedOutline(context, bounds, mixColor(palette.border(), palette.focus(), focused), radius);
    }

    public static void toggle(UiContext context, Bounds bounds, boolean selected, boolean hovered,
                              boolean enabled, boolean focused) {
        toggle(context, bounds, selected ? 1 : 0, hovered ? 1 : 0, enabled, focused ? 1 : 0);
    }

    public static void toggle(UiContext context, Bounds bounds, double selected, double hovered,
                             boolean enabled, double focused) {
        UiPalette palette = context.theme().palette();
        Insets padding = context.theme().tokens().buttonPadding();
        int trackHeight = Math.max(10, Math.min(14, bounds.height() - padding.top() - padding.bottom()));
        int trackY = bounds.centerY() - trackHeight / 2;
        int trackX = bounds.x() + padding.left();
        int trackColor = !enabled ? palette.disabled()
                : mixColor(mixColor(palette.button(), palette.buttonHover(), hovered), palette.accent(), selected);
        Bounds track = new Bounds(trackX, trackY, 24, trackHeight);
        int radius = context.theme().tokens().radii().pill();
        roundedRect(context, track, trackColor, radius);
        roundedOutline(context, track, mixColor(palette.border(), palette.focus(), focused), radius);
        int knob = Math.max(6, trackHeight - 4);
        int knobX = (int) Math.round(trackX + 2 + selected * (24 - knob - 4));
        roundedRect(context, new Bounds(knobX, trackY + 2, knob, knob),
                enabled ? palette.text() : palette.mutedText(), context.theme().tokens().radii().pill());
    }

    public static void radio(UiContext context, Bounds indicator, boolean selected, boolean hovered,
                             boolean enabled, boolean focused) {
        radio(context, indicator, selected ? 1 : 0, hovered ? 1 : 0, enabled, focused ? 1 : 0);
    }

    /** Round radio mark. Selection only fills the ring, so the mark never changes size. */
    public static void radio(UiContext context, Bounds indicator, double selected, double hovered,
                             boolean enabled, double focused) {
        UiPalette palette = context.theme().palette();
        int radius = Math.min(indicator.width(), indicator.height()) / 2;
        int ring = !enabled ? palette.disabled()
                : mixColor(mixColor(mixColor(palette.mutedText(), palette.text(), hovered), palette.text(), selected),
                palette.focus(), focused);
        if (selected > 0) {
            roundedRect(context, indicator, withAlpha(enabled ? palette.text() : palette.disabled(), selected), radius);
        }
        roundedOutline(context, indicator, ring, radius);
    }

    public static void chip(UiContext context, Bounds bounds, boolean selected, boolean hovered,
                            boolean enabled, boolean focused) {
        chip(context, bounds, selected ? 1 : 0, hovered ? 1 : 0, enabled, focused ? 1 : 0);
    }

    public static void chip(UiContext context, Bounds bounds, double selected, double hovered,
                            boolean enabled, double focused) {
        UiPalette palette = context.theme().palette();
        int fill = !enabled ? palette.disabled()
                : mixColor(mixColor(palette.card(), palette.buttonHover(), hovered), palette.accent(), selected);
        int radius = context.theme().tokens().radii().pill();
        roundedRect(context, bounds, fill, radius);
        roundedOutline(context, bounds, mixColor(palette.border(), palette.focus(), focused), radius);
    }

    public static void tab(UiContext context, Bounds bounds, boolean selected, boolean focused) {
        tab(context, bounds, selected ? 1 : 0, focused ? 1 : 0);
    }

    public static void tab(UiContext context, Bounds bounds, double selected, double focused) {
        tab(context, bounds, selected, focused, true, true);
    }

    /** Paints one segment of a tab strip, rounding only the exposed outer corners. */
    public static void tab(UiContext context, Bounds bounds, double selected, double focused,
                           boolean roundLeft, boolean roundRight) {
        UiPalette palette = context.theme().palette();
        int radius = context.theme().tokens().radii().small();
        int fill = mixColor(palette.panel(), palette.card(), selected);
        int border = mixColor(palette.border(), palette.focus(), focused);
        roundedRect(context, bounds, fill, radius);
        roundedOutline(context, bounds, border, radius);
        int squareWidth = Math.min(radius, bounds.width());
        if (!roundLeft && squareWidth > 0) {
            rect(context, new Bounds(bounds.x(), bounds.y(), squareWidth, bounds.height()), fill);
            rect(context, new Bounds(bounds.x(), bounds.y(), 1, bounds.height()), border);
            rect(context, new Bounds(bounds.x(), bounds.y(), squareWidth, 1), border);
            rect(context, new Bounds(bounds.x(), bounds.bottom() - 1, squareWidth, 1), border);
        }
        if (!roundRight && squareWidth > 0) {
            rect(context, new Bounds(bounds.right() - squareWidth, bounds.y(), squareWidth, bounds.height()), fill);
            rect(context, new Bounds(bounds.right() - 1, bounds.y(), 1, bounds.height()), border);
            rect(context, new Bounds(bounds.right() - squareWidth, bounds.y(), squareWidth, 1), border);
            rect(context, new Bounds(bounds.right() - squareWidth, bounds.bottom() - 1, squareWidth, 1), border);
        }
        if (selected > 0) rect(context, new Bounds(bounds.x(), bounds.bottom() - 2, bounds.width(), 2),
                withAlpha(palette.accent(), selected));
    }

    public static void input(UiContext context, Bounds bounds, boolean hovered,
                             boolean enabled, boolean focused) {
        input(context, bounds, hovered ? 1 : 0, enabled, focused ? 1 : 0);
    }

    public static void input(UiContext context, Bounds bounds, double hovered,
                             boolean enabled, double focused) {
        input(context, bounds, hovered, enabled, focused, null);
    }

    public static void input(UiContext context, Bounds bounds, double hovered,
                             boolean enabled, double focused, Integer backgroundOverride) {
        UiPalette palette = context.theme().palette();
        int radius = context.theme().tokens().radii().small();
        int background = backgroundOverride == null ? palette.card()
                : withAlpha(backgroundOverride, context.opacity());
        int hoveredBackground = backgroundOverride == null ? palette.panel()
                : withAlpha(palette.panel(), context.opacity());
        roundedRect(context, bounds, !enabled ? palette.disabled()
                : mixColor(background, hoveredBackground, hovered), radius);
        roundedOutline(context, bounds, mixColor(palette.border(), palette.focus(), focused), radius);
    }

    /** Paints a tooltip in the screen's top overlay layer. */
    public static void tooltip(UiContext context, Tooltip tooltip, int mouseX, int mouseY) {
        context.graphics().pose().pushPose();
        context.graphics().pose().translate(0.0F, 0.0F, TOOLTIP_Z_INDEX);
        try {
            UiPalette palette = context.theme().palette();
            UiTokens tokens = context.theme().tokens();
            Insets padding = tokens.cardPadding();
            float scale = tokens.typography().smallScale();
            int maxRenderedWidth = Math.max(1, Math.min(240, context.screenWidth() - 16)
                    - padding.left() - padding.right());
            int maxFontWidth = Math.max(1, (int) Math.floor(maxRenderedWidth / scale));
            List<FormattedCharSequence> lines = new ArrayList<>();
            for (Component component : tooltip.lines()) {
                List<FormattedCharSequence> wrapped = context.font().split(component, maxFontWidth);
                lines.addAll(wrapped.isEmpty() ? List.of(FormattedCharSequence.EMPTY) : wrapped);
            }
            int textWidth = 0;
            for (FormattedCharSequence line : lines) {
                textWidth = Math.max(textWidth, Math.round(context.font().width(line) * scale));
            }
            int lineHeight = UiText.height(context.font(), context.theme(), UiTextStyle.SMALL);
            int gap = tokens.smallGap();
            int width = Math.min(context.screenWidth(), textWidth + padding.left() + padding.right());
            int height = lines.size() * lineHeight + Math.max(0, lines.size() - 1) * gap
                    + padding.top() + padding.bottom();
            int x = mouseX + 10;
            if (x + width > context.screenWidth() - 4) x = mouseX - width - 10;
            x = Math.max(4, Math.min(x, context.screenWidth() - width - 4));
            int y = mouseY + 10;
            if (y + height > context.screenHeight() - 4) y = mouseY - height - 8;
            y = Math.max(4, Math.min(y, context.screenHeight() - height - 4));

            Bounds bounds = new Bounds(x, y, width, height);
            int radius = tokens.radii().small();
            roundedRect(context, bounds, palette.card(), radius);
            roundedOutline(context, bounds, palette.border(), radius);
            int textY = y + padding.top();
            for (FormattedCharSequence line : lines) {
                text(context, line, x + padding.left(), textY, palette.text(), UiTextStyle.SMALL);
                textY += lineHeight + gap;
            }
        } finally {
            context.graphics().pose().popPose();
        }
    }

    public static int mixColor(int from, int to, double progress) {
        double amount = Math.max(0, Math.min(1, progress));
        int a = interpolateChannel(from >>> 24, to >>> 24, amount);
        int r = interpolateChannel((from >>> 16) & 0xFF, (to >>> 16) & 0xFF, amount);
        int g = interpolateChannel((from >>> 8) & 0xFF, (to >>> 8) & 0xFF, amount);
        int b = interpolateChannel(from & 0xFF, to & 0xFF, amount);
        return a << 24 | r << 16 | g << 8 | b;
    }

    public static int withAlpha(int color, double opacity) {
        int alpha = interpolateChannel(0, color >>> 24, opacity);
        return color & 0x00FFFFFF | alpha << 24;
    }

    private static int interpolateChannel(int from, int to, double amount) {
        return (int) Math.round(from + (to - from) * amount);
    }

    public static void text(UiContext context, String value, int x, int y, int color) {
        text(context, value, x, y, color, UiTextStyle.BODY);
    }

    public static void text(UiContext context, String value, int x, int y, int color, UiTextStyle style) {
        withTextScale(context, style, x, y, color,
                (graphics, textColor) -> graphics.drawString(context.font(), value, 0, 0,
                        textColor, false));
    }

    public static void textCentered(UiContext context, String value, int centerX, int y, int color) {
        textCentered(context, value, centerX, y, color, UiTextStyle.BODY);
    }

    public static void textCentered(UiContext context, String value, int centerX, int y, int color,
                                    UiTextStyle style) {
        int xOffset = -context.font().width(value) / 2;
        withTextScale(context, style, centerX, y, color,
                (graphics, textColor) -> graphics.drawString(context.font(), value, xOffset, 0,
                        textColor, false));
    }

    public static void text(UiContext context, net.minecraft.network.chat.Component value,
                            int x, int y, int color) {
        text(context, value, x, y, color, UiTextStyle.BODY);
    }

    public static void text(UiContext context, net.minecraft.network.chat.Component value,
                            int x, int y, int color, UiTextStyle style) {
        withTextScale(context, style, x, y, color,
                (graphics, textColor) -> graphics.drawString(context.font(), value, 0, 0,
                        textColor, false));
    }

    public static void textCentered(UiContext context, net.minecraft.network.chat.Component value,
                                    int centerX, int y, int color) {
        textCentered(context, value, centerX, y, color, UiTextStyle.BODY);
    }

    public static void textCentered(UiContext context, net.minecraft.network.chat.Component value,
                                    int centerX, int y, int color, UiTextStyle style) {
        int xOffset = -context.font().width(value) / 2;
        withTextScale(context, style, centerX, y, color,
                (graphics, textColor) -> graphics.drawString(context.font(), value, xOffset, 0,
                        textColor, false));
    }

    public static void text(UiContext context, FormattedCharSequence value, int x, int y, int color,
                            UiTextStyle style) {
        withTextScale(context, style, x, y, color,
                (graphics, textColor) -> graphics.drawString(context.font(), value, 0, 0,
                        textColor, false));
    }

    private static void withTextScale(UiContext context, UiTextStyle style, int x, int y,
                                      int color, BiConsumer<GuiGraphics, Integer> drawing) {
        int textColor = withAlpha(color, context.opacity());
        // Font.adjustColor treats alpha 0..3 as an implicit opaque RGB color.
        // Skip these effectively invisible glyphs so a fade cannot flash back to full opacity.
        if ((textColor >>> 24) < 4) return;
        GuiGraphics graphics = context.graphics();
        float scale = context.theme().tokens().typography().scale(style);
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(scale, scale, 1);
        drawing.accept(graphics, textColor);
        graphics.pose().popPose();
    }
}
