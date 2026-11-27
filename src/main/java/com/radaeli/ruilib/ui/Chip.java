package com.radaeli.ruilib.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

import java.util.Objects;
import java.util.function.Consumer;

/** Compact selectable pill used for filters and short options. */
public final class Chip extends UiComponent {
    private final Component label;
    private final Consumer<Boolean> onChange;
    private boolean selected;
    private boolean pressed;

    public Chip(Component label, Consumer<Boolean> onChange) {
        this(label, false, onChange);
    }

    public Chip(Component label, boolean selected, Consumer<Boolean> onChange) {
        this.label = Objects.requireNonNull(label, "label");
        this.selected = selected;
        this.onChange = Objects.requireNonNull(onChange, "onChange");
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    protected UiSize measureIntrinsic(Font font, UiTheme theme, int maxWidth, int maxHeight) {
        Insets padding = theme.tokens().buttonPadding();
        return new UiSize(UiText.width(font, label, theme) + padding.left() + padding.right(),
                UiText.height(font, theme) + Math.max(4, padding.top() + padding.bottom()))
                .constrain(maxWidth, maxHeight);
    }

    @Override
    public void render(UiContext context) {
        boolean hovered = isEnabled() && bounds().contains(context.mouseX(), context.mouseY());
        UiPaint.chip(context, bounds(), animate(context, "selected", selected ? 1 : 0),
                animate(context, "hover", hovered ? 1 : 0), isEnabled(),
                animate(context, "focus", isFocused() ? 1 : 0));
        UiPaint.controlLabel(context, label, bounds(), 0,
                context.theme().skin().buttonText(context.theme(),isEnabled(),selected));
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
        if (isEnabled() && bounds().contains(mouseX, mouseY)) {
            selected = !selected;
            onChange.accept(selected);
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!isEnabled() || !isFocused() || (keyCode != 257 && keyCode != 335 && keyCode != 32)) return false;
        selected = !selected;
        onChange.accept(selected);
        return true;
    }

    @Override
    public boolean isFocusable() {
        return true;
    }
}
