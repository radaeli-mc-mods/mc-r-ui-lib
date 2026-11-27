package com.radaeli.ruilib.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/** Selectable card that renders a vanilla item icon, its name, and its tooltip. */
public final class ItemCard extends UiComponent {
    private static final int ICON_SIZE = 16;
    private static final int HORIZONTAL_PADDING = 6;
    private static final int GAP = 6;
    private static final int MINIMUM_WIDTH = 52;
    private static final int MINIMUM_HEIGHT = 28;

    private final ItemStack stack;
    private final Component label;
    private final Runnable onClick;
    private boolean selected;
    private boolean pressed;

    public ItemCard(ItemStack stack, Component label) {
        this(stack, label, false, () -> {});
    }

    public ItemCard(ItemStack stack, Component label, Runnable onClick) {
        this(stack, label, false, onClick);
    }

    public ItemCard(ItemStack stack, Component label, boolean selected, Runnable onClick) {
        this.stack = Objects.requireNonNull(stack, "stack").copy();
        this.label = Objects.requireNonNull(label, "label");
        this.selected = selected;
        this.onClick = Objects.requireNonNull(onClick, "onClick");
    }

    public ItemStack stack() {
        return stack.copy();
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    protected UiSize measureIntrinsic(Font font, UiTheme theme, int maxWidth, int maxHeight) {
        int width = HORIZONTAL_PADDING * 2 + ICON_SIZE + GAP + UiText.width(font, label, theme);
        int height = Math.max(ICON_SIZE, UiText.height(font, theme)) + 8;
        return new UiSize(Math.max(MINIMUM_WIDTH, width), Math.max(MINIMUM_HEIGHT, height))
                .constrain(maxWidth, maxHeight);
    }

    @Override
    public void render(UiContext context) {
        boolean hovered = isEnabled() && bounds().contains(context.mouseX(), context.mouseY());
        double hover = animate(context, "hover", hovered ? 1 : 0);
        double focus = animate(context, "focus", isFocused() ? 1 : 0);
        double selection = animate(context, "selected", selected ? 1 : 0);
        double press = animate(context, "press", pressed ? 1 : 0);
        if (selected) {
            UiPaint.chip(context, bounds(), selection, hover, isEnabled(), focus);
        } else {
            UiPaint.button(context, bounds(), hover, press, isEnabled(), focus);
        }

        int iconX = bounds().x() + HORIZONTAL_PADDING;
        int iconY = bounds().centerY() - ICON_SIZE / 2;
        ItemRenderer.render(context, stack, iconX, iconY);

        int color = isEnabled() ? context.theme().palette().text() : context.theme().palette().mutedText();
        int textX = iconX + ICON_SIZE + GAP;
        int textY = bounds().centerY() - UiText.height(context) / 2;
        if(context.theme().skin().pixelated())color=context.theme().skin().buttonText(context.theme(),isEnabled(),selected);
        UiPaint.text(context,UiText.ellipsize(context.font(),label,Math.max(0,(int)((bounds().right()-textX-HORIZONTAL_PADDING)/context.theme().tokens().typography().bodyScale()))),textX,textY,color,UiTextStyle.BODY);
        if (hovered) ItemRenderer.renderTooltip(context, stack);
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
        if (isEnabled() && bounds().contains(mouseX, mouseY)) onClick.run();
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!isEnabled() || !isFocused()
                || (keyCode != 257 && keyCode != 335 && keyCode != 32)) return false;
        onClick.run();
        return true;
    }

    @Override
    public boolean isFocusable() {
        return true;
    }
}
