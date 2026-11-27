package com.radaeli.ruilib.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

import java.util.Objects;
import java.util.function.Consumer;

/** Shared layout and input behavior for framed presentation components. */
abstract class UiFrame extends UiComponent {
    private final UiComponent content;
    private final Insets paddingOverride;

    UiFrame(UiComponent content, Insets padding) {
        this.content = Objects.requireNonNull(content, "content");
        this.paddingOverride = padding;
    }

    protected UiComponent content() {
        return content;
    }

    @Override
    public void visitComponents(Consumer<UiComponent> visitor) {
        visitor.accept(this);
        content.visitComponents(visitor);
    }

    protected Insets padding(UiTheme theme) {
        return paddingOverride == null ? defaultPadding(theme) : paddingOverride;
    }

    protected Insets defaultPadding(UiTheme theme) {
        return theme.tokens().panelPadding();
    }

    protected int contentTopOffset(Font font, UiTheme theme) {
        return 0;
    }

    @Override
    protected UiSize measureIntrinsic(Font font, UiTheme theme, int maxWidth, int maxHeight) {
        Insets padding = padding(theme);
        int top = padding.top() + contentTopOffset(font, theme);
        UiSize child = content.measure(font, theme,
                Math.max(0, maxWidth - padding.left() - padding.right()),
                Math.max(0, maxHeight - top - padding.bottom()));
        return new UiSize(child.width() + padding.left() + padding.right(),
                child.height() + top + padding.bottom()).constrain(maxWidth, maxHeight);
    }

    @Override
    protected void layoutChildren(Font font, UiTheme theme) {
        Insets padding = padding(theme);
        int top = padding.top() + contentTopOffset(font, theme);
        Bounds inner = new Bounds(bounds().x() + padding.left(), bounds().y() + top,
                Math.max(0, bounds().width() - padding.left() - padding.right()),
                Math.max(0, bounds().height() - top - padding.bottom()));
        content.layout(font, theme, inner);
    }

    @Override public UiComponent inspectionHitTest(double x,double y){if(!bounds().contains(x,y))return null;var hit=content.inspectionHitTest(x,y);return hit==null?this:hit;}
    @Override
    public UiComponent hitTest(double mouseX, double mouseY) {
        if (!isEnabled() || !bounds().contains(mouseX, mouseY)) return null;
        UiComponent hit = content.hitTest(mouseX, mouseY);
        return hit == null ? this : hit;
    }

    @Override
    public void visitFocusable(Consumer<UiComponent> visitor) {
        if (isEnabled()) content.visitFocusable(visitor);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return isEnabled() && bounds().contains(mouseX, mouseY)
                && content.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return content.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return content.charTyped(codePoint, modifiers);
    }
}
