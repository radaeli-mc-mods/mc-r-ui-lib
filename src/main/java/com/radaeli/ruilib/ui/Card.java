package com.radaeli.ruilib.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

/** Framed content group with an optional title and selectable/hover presentation states. */
public final class Card extends UiFrame {
    private final Component title;
    private boolean hoverEnabled = true;
    private boolean selected;

    public Card(UiComponent content) {
        this(null, content, null);
    }

    public Card(Component title, UiComponent content) {
        this(title, content, null);
    }

    public Card(Component title, UiComponent content, Insets padding) {
        super(content, padding);
        this.title = title;
    }

    @Override
    protected Insets defaultPadding(UiTheme theme) {
        return theme.tokens().cardPadding();
    }

    public Card setHoverEnabled(boolean enabled) {
        this.hoverEnabled = enabled;
        return this;
    }

    public Card setSelected(boolean selected) {
        this.selected = selected;
        return this;
    }

    @Override
    protected int contentTopOffset(Font font, UiTheme theme) {
        return title == null ? 0 : UiText.height(font, theme, UiTextStyle.HEADING) + theme.tokens().smallGap();
    }

    @Override
    protected UiSize measureIntrinsic(Font font, UiTheme theme, int maxWidth, int maxHeight) {
        UiSize child = super.measureIntrinsic(font, theme, maxWidth, maxHeight);
        if (title == null) return child;
        Insets padding = padding(theme);
        int titleWidth = UiText.width(font, title, theme, UiTextStyle.HEADING) + padding.left() + padding.right();
        return new UiSize(Math.max(child.width(), titleWidth), child.height()).constrain(maxWidth, maxHeight);
    }

    @Override
    public void render(UiContext context) {
        boolean hovered = hoverEnabled && isEnabled() && bounds().contains(context.mouseX(), context.mouseY());
        UiPaint.card(context, bounds(), animate(context, "hover", hovered ? 1 : 0),
                animate(context, "selected", selected ? 1 : 0),
                animate(context, "focus", isFocused() ? 1 : 0));
        Insets padding = padding(context.theme());
        if (title != null) {
            if(context.theme().skin().pixelated())context.theme().skin().surface(context,
                new Bounds(bounds().x()+padding.left()-3,bounds().y()+padding.top()-3,
                Math.max(0,bounds().width()-padding.left()-padding.right()+6),UiText.height(context)+6),SurfaceVariant.TITLE_PLATE,null);
            UiPaint.text(context, title, bounds().x() + padding.left(),
                    bounds().y() + padding.top(), context.theme().palette().text(), UiTextStyle.HEADING);
        }
        content().render(selected?context.selectedSurface():context);
    }

    public Component title() {
        return title;
    }

    public UiComponent contentComponent() {
        return content();
    }
}
