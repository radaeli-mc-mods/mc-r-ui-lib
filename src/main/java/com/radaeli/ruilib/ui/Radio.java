package com.radaeli.ruilib.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

import java.util.Objects;

/**
 * One exclusive option with a round mark, a label and an optional right-aligned hint. Selecting never
 * clears it; group several with {@link RadioGroup}, or keep the shared value yourself.
 */
public final class Radio extends UiComponent {
    /** Mark size in pixels. Fixed, so labels line up whether or not the option is selected. */
    public static final int INDICATOR = 8;
    private final Component label;
    private final Component hint;
    private final Runnable onSelect;
    private boolean selected;
    private boolean pressed;

    public Radio(Component label, boolean selected, Runnable onSelect) {
        this(label, Component.empty(), selected, onSelect);
    }

    public Radio(Component label, Component hint, boolean selected, Runnable onSelect) {
        this.label = Objects.requireNonNull(label, "label");
        this.hint = Objects.requireNonNull(hint, "hint");
        this.selected = selected;
        this.onSelect = Objects.requireNonNull(onSelect, "onSelect");
    }

    public boolean isSelected() {
        return selected;
    }

    /** Updates the mark without invoking the callback. */
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    protected UiSize measureIntrinsic(Font font, UiTheme theme, int maxWidth, int maxHeight) {
        return rowSize(font, theme, label, hint).constrain(maxWidth, maxHeight);
    }

    @Override
    public void render(UiContext context) {
        boolean hovered = isEnabled() && bounds().contains(context.mouseX(), context.mouseY());
        paintRow(context, bounds(), label, hint, animate(context, "selected", selected ? 1 : 0),
                animate(context, "hover", hovered ? 1 : 0), isEnabled(),
                animate(context, "focus", isFocused() ? 1 : 0));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isEnabled() || button != 0 || !bounds().contains(mouseX, mouseY)) return false;
        pressed = true;
        setFocused(true);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!pressed || button != 0) return false;
        pressed = false;
        if (isEnabled() && bounds().contains(mouseX, mouseY)) select();
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!isEnabled() || !isFocused() || (keyCode != 257 && keyCode != 335 && keyCode != 32)) return false;
        select();
        return true;
    }

    private void select() {
        if (selected) return;
        selected = true;
        onSelect.run();
    }

    @Override
    public boolean isFocusable() {
        return true;
    }

    static UiSize rowSize(Font font, UiTheme theme, Component label, Component hint) {
        Insets padding = theme.tokens().buttonPadding();
        int gap = theme.tokens().smallGap();
        int hintWidth = hint.getString().isEmpty() ? 0 : gap * 2 + UiText.width(font, hint, theme);
        return new UiSize(padding.left() + INDICATOR + gap + UiText.width(font, label, theme) + hintWidth
                + padding.right(), Math.max(INDICATOR, UiText.height(font, theme)) + 4);
    }

    /** Shared by {@link Radio} and {@link RadioGroup}: mark, label, then the hint against the right edge. */
    static void paintRow(UiContext context, Bounds bounds, Component label, Component hint, double selected,
                         double hovered, boolean enabled, double focused) {
        UiPalette palette = context.theme().palette();
        Insets padding = context.theme().tokens().buttonPadding();
        int gap = context.theme().tokens().smallGap();
        if (hovered > 0) {
            UiPaint.roundedRect(context, bounds, UiPaint.withAlpha(palette.buttonHover(), hovered * 0.5),
                    context.theme().tokens().radii().small());
        }
        int markX = bounds.x() + padding.left();
        UiPaint.radio(context, new Bounds(markX, bounds.centerY() - INDICATOR / 2, INDICATOR, INDICATOR),
                selected, hovered, enabled, focused);
        int textY = bounds.centerY() - UiText.height(context) / 2;
        int labelColor = !enabled ? palette.disabled()
                : UiPaint.mixColor(palette.mutedText(), palette.text(), Math.max(selected, hovered));
        UiPaint.text(context, label, markX + INDICATOR + gap, textY, labelColor);
        if (!hint.getString().isEmpty()) {
            int hintX = bounds.right() - padding.right() - UiText.width(context.font(), hint, context.theme());
            UiPaint.text(context, hint, hintX, textY, enabled ? palette.mutedText() : palette.disabled());
        }
    }
}
