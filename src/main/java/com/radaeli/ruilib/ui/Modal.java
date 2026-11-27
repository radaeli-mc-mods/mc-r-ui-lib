package com.radaeli.ruilib.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import java.util.Objects;
import java.util.function.Consumer;

/** Centered modal content rendered as an overlay by {@link UiScreen}. */
public final class Modal extends UiComponent {
    private final Component title;
    private final UiComponent content;
    private UiComponent headerAction;
    private final int preferredDialogWidth;
    private final int preferredDialogHeight;
    private boolean closeOnOutsideClick;
    private boolean closing;
    private double transitionOpacity;
    private Runnable onClose = () -> { };

    public Modal(Component title, UiComponent content) {
        this(title, content, 240, 140);
    }

    public Modal(Component title, UiComponent content, int width, int height) {
        this.title = Objects.requireNonNull(title, "title");
        this.content = Objects.requireNonNull(content, "content");
        if (width < 80 || height < 48) throw new IllegalArgumentException("Modal dimensions are too small");
        this.preferredDialogWidth = width;
        this.preferredDialogHeight = height;
    }

    public Modal setCloseOnOutsideClick(boolean close) {
        closeOnOutsideClick = close;
        return this;
    }

    public Modal setOnClose(Runnable onClose) {
        this.onClose = Objects.requireNonNull(onClose, "onClose");
        return this;
    }

    /** Places an optional interactive component at the top-right of the title heading. */
    public Modal setHeaderAction(UiComponent action) {
        this.headerAction = Objects.requireNonNull(action, "action");
        return this;
    }

    public Component title() { return title; }
    public UiComponent content() { return content; }
    public boolean closeOnOutsideClick() { return closeOnOutsideClick; }
    public void closed() { onClose.run(); }

    @Override
    public void visitComponents(Consumer<UiComponent> visitor) {
        visitor.accept(this);
        if (headerAction != null) headerAction.visitComponents(visitor);
        content.visitComponents(visitor);
    }
    void beginOpen() {
        closing = false;
        transitionOpacity = 0;
        clearAnimation("modal-opacity");
        clearContentFocus();
    }
    void beginClose() { closing = true; }
    boolean closeTransitionComplete() { return closing && transitionOpacity <= 0.001; }

    void clearContentFocus() {
        if (headerAction != null) headerAction.visitFocusable(component -> component.setFocused(false));
        content.visitFocusable(component -> component.setFocused(false));
    }

    public void layoutInScreen(Font font, UiTheme theme, int screenWidth, int screenHeight) {
        int dialogWidth = Math.min(preferredDialogWidth, Math.max(1, screenWidth - 16));
        int dialogHeight = Math.min(preferredDialogHeight, Math.max(1, screenHeight - 16));
        layout(font, theme, new Bounds((screenWidth - dialogWidth) / 2,
                (screenHeight - dialogHeight) / 2, dialogWidth, dialogHeight));
    }

    @Override
    protected UiSize measureIntrinsic(Font font, UiTheme theme, int maxWidth, int maxHeight) {
        return new UiSize(Math.min(preferredDialogWidth, maxWidth),
                Math.min(preferredDialogHeight, maxHeight));
    }

    @Override
    protected void layoutChildren(Font font, UiTheme theme) {
        Insets padding = theme.tokens().panelPadding();
        int headingHeight = headingContentHeight(font, theme);
        int headingBottom = bounds().y() + padding.top() + headingHeight;
        if (headerAction != null) {
            UiSize actionSize = headerAction.measure(font, theme,
                    Math.max(0, bounds().width() - padding.left() - padding.right()),
                    Math.max(0, bounds().height() - padding.top() - padding.bottom()));
            headerAction.layout(font, theme, new Bounds(bounds().right() - padding.right() - actionSize.width(),
                    bounds().y() + padding.top() + Math.max(0, (headingHeight - actionSize.height()) / 2),
                    actionSize.width(), actionSize.height()));
        }
        content.layout(font, theme, new Bounds(bounds().x() + padding.left(),
                headingBottom + theme.tokens().mediumGap(),
                Math.max(0, bounds().width() - padding.left() - padding.right()),
                Math.max(0, bounds().bottom() - padding.bottom() - headingBottom - theme.tokens().mediumGap())));
    }

    private int headingContentHeight(Font font, UiTheme theme) {
        int titleHeight = UiText.height(font, theme, UiTextStyle.TITLE);
        if (headerAction == null) return titleHeight;
        int availableWidth = Math.max(0, bounds().width() - theme.tokens().panelPadding().left()
                - theme.tokens().panelPadding().right());
        int availableHeight = Math.max(0, bounds().height() - theme.tokens().panelPadding().top()
                - theme.tokens().panelPadding().bottom());
        return Math.max(titleHeight,
                headerAction.measure(font, theme, availableWidth, availableHeight).height());
    }

    @Override
    public void render(UiContext context) {
        double opacity = animateFrom(context, "modal-opacity", closing ? 0 : 1, closing ? 1 : 0);
        transitionOpacity = opacity;
        if (opacity <= 0) return;
        // Vanilla items render near z=150. Place the entire modal above background items,
        // while keeping its own item icons below UiScreen's z=400 select overlay.
        context.graphics().pose().pushPose();
        context.graphics().pose().translate(0, 0, 200);
        try {
        UiPaint.rect(context, new Bounds(0, 0, context.screenWidth(), context.screenHeight()),
                UiPaint.withAlpha(context.theme().palette().overlay(), opacity));
        UiPaint.panel(context, bounds(), opacity);
        Insets padding = context.theme().tokens().panelPadding();
        if(context.theme().skin().pixelated()){
            int titleWidth=Math.min(bounds().width()-padding.left()-padding.right()-(headerAction==null?0:headerAction.bounds().width()+4),UiText.width(context.font(),title,context.theme())+6);
            context.theme().skin().surface(context,new Bounds(bounds().x()+padding.left()-3,bounds().y()+padding.top()-3,Math.max(0,titleWidth),UiText.height(context)+6),SurfaceVariant.TITLE_PLATE,null);
        }
        if(context.theme().skin().pixelated())UiPaint.text(context,UiText.ellipsize(context.font(),title,Math.max(0,bounds().width()-padding.left()-padding.right()-(headerAction==null?0:headerAction.bounds().width()+4))),bounds().x()+padding.left(),bounds().y()+padding.top(),context.theme().palette().text(),UiTextStyle.TITLE);
        else UiPaint.text(context, title, bounds().x() + padding.left(), bounds().y() + padding.top(),
                UiPaint.withAlpha(context.theme().palette().text(), opacity), UiTextStyle.TITLE);
        int dividerY = bounds().y() + padding.top() + headingContentHeight(context.font(), context.theme())
                + context.theme().tokens().smallGap();
        UiPaint.rect(context, new Bounds(bounds().x() + padding.left(), dividerY,
                Math.max(0, bounds().width() - padding.left() - padding.right()), 1),
                UiPaint.withAlpha(context.theme().palette().divider(), opacity));
        if (headerAction != null) headerAction.render(context.withOpacity(opacity));
        content.render(context.withOpacity(opacity));
        } finally { context.graphics().pose().popPose(); }
    }

    @Override public UiComponent inspectionHitTest(double x,double y){
        if(!bounds().contains(x,y))return null;
        if(headerAction!=null){var hit=headerAction.inspectionHitTest(x,y);if(hit!=null)return hit;}
        var hit=content.inspectionHitTest(x,y);return hit==null?this:hit;
    }
    @Override
    public UiComponent hitTest(double mouseX, double mouseY) {
        if (!bounds().contains(mouseX, mouseY)) return null;
        if (headerAction != null) {
            UiComponent target = headerAction.hitTest(mouseX, mouseY);
            if (target != null) return target;
        }
        UiComponent target = content.hitTest(mouseX, mouseY);
        return target == null ? this : target;
    }

    @Override
    public void visitFocusable(Consumer<UiComponent> visitor) {
        if (isEnabled()) {
            if (headerAction != null) headerAction.visitFocusable(visitor);
            content.visitFocusable(visitor);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return bounds().contains(mouseX, mouseY) && content.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return content.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return content.charTyped(codePoint, modifiers);
    }

    public boolean shouldCloseFromOutsideClick(double mouseX, double mouseY) {
        return closeOnOutsideClick && !bounds().contains(mouseX, mouseY);
    }
}
