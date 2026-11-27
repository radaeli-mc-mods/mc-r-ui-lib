package com.radaeli.ruilib.ui;

import net.minecraft.client.gui.Font;

/** Vertically stacks children in a clipped viewport with mouse-wheel scrolling. */
public final class ScrollView extends UiContainer {
    private final Integer gapOverride;
    private int scrollOffset;
    private double visualScrollOffset;
    private int contentHeight;
    private Font lastFont;
    private UiTheme lastTheme;
    private boolean draggingScrollbar;
    private SurfaceVariant surface=SurfaceVariant.DEFAULT;
    public ScrollView setSurface(SurfaceVariant surface){this.surface=java.util.Objects.requireNonNull(surface);return this;}

    public ScrollView() {
        this((Integer) null);
    }

    public ScrollView(int gap) {
        if (gap < 0) throw new IllegalArgumentException("gap cannot be negative");
        this.gapOverride = gap;
    }

    private ScrollView(Integer gap) {
        this.gapOverride = gap;
    }

    public int scrollOffset() {
        return scrollOffset;
    }

    public void setScrollOffset(int scrollOffset) {
        this.scrollOffset = Math.max(0, Math.min(scrollOffset, maxScrollOffset()));
        relayout();
    }

    public int maxScrollOffset() {
        return Math.max(0, contentHeight - bounds().height());
    }

    @Override
    protected UiSize measureIntrinsic(Font font, UiTheme theme, int maxWidth, int maxHeight) {
        int gap = gap(theme);
        int width = 0;
        int height = 0;
        for (UiComponent child : children()) {
            UiSize size = contentSize(child, font, theme, maxWidth, maxHeight);
            width = Math.max(width, size.width());
            height += size.height();
        }
        if (children().size() > 1) height += gap * (children().size() - 1);
        return new UiSize(width, height).constrain(maxWidth, maxHeight);
    }

    @Override
    protected void layoutChildren(Font font, UiTheme theme) {
        int gap = gap(theme);
        int inset=theme.skin().pixelated()?theme.skin().metrics().frameInset():0;
        int gutter=theme.skin().pixelated()?theme.skin().metrics().scrollbarHitWidth():0;
        int available=Math.max(0,bounds().width()-inset*2-gutter);
        lastFont = font;
        lastTheme = theme;
        contentHeight = 0;
        UiSize[] sizes = new UiSize[children().size()];
        for (int i = 0; i < children().size(); i++) {
            sizes[i] = contentSize(children().get(i), font, theme, available, 8192);
            contentHeight += sizes[i].height();
        }
        if (sizes.length > 1) contentHeight += gap * (sizes.length - 1);
        contentHeight+=inset*2;
        scrollOffset = Math.min(scrollOffset, maxScrollOffset());
        if(theme.skin().pixelated())visualScrollOffset=scrollOffset;

        int y = (int) Math.round(bounds().y() + inset - visualScrollOffset);
        for (int i = 0; i < children().size(); i++) {
            UiComponent child = children().get(i);
            UiSize size = sizes[i];
            int childWidth = child.preferredWidth() == null
                    ? available : Math.min(size.width(), available);
            int x = Column.alignedStart(bounds().x()+inset, available, childWidth,
                    child.preferredWidth() == null ? Alignment.STRETCH : Alignment.START);
            child.layout(font, theme, new Bounds(x, y, childWidth, size.height()));
            y += size.height() + gap;
        }
    }

    private static UiSize contentSize(UiComponent child, Font font, UiTheme theme,
                                     int maxWidth, int maxHeight) {
        // A container's preferred height describes its usual viewport; scrolling needs its full content height.
        if (child instanceof UiContainer) return child.measureNatural(font, theme, maxWidth, maxHeight);
        return child.measure(font, theme, maxWidth, maxHeight);
    }

    private void relayout() {
        if (lastFont != null && lastTheme != null) layoutChildren(lastFont, lastTheme);
    }

    private int gap(UiTheme theme) {
        return gapOverride == null ? theme.tokens().smallGap() : gapOverride;
    }

    @Override
    public void render(UiContext context) {
        double previousOffset = visualScrollOffset;
        visualScrollOffset = animate(context, "scroll-offset", scrollOffset);
        if (Math.abs(previousOffset - visualScrollOffset) >= 0.01
                && lastFont != null && lastTheme != null) layoutChildren(lastFont, lastTheme);
        if(surface!=SurfaceVariant.PLAIN){if(!context.theme().skin().surface(context,bounds(),surface==SurfaceVariant.DEFAULT?SurfaceVariant.INSET:surface,WorkshopSkin.WELL))UiPaint.panel(context,bounds());}
        Bounds viewport=context.theme().skin().pixelated()?bounds().inset(Insets.all(context.theme().skin().metrics().frameInset())):bounds();
        context.graphics().enableScissor(viewport.x(),viewport.y(),viewport.right(),viewport.bottom());
        try {
            super.render(context);
        } finally {
            context.graphics().disableScissor();
        }
        if (maxScrollOffset() > 0 && bounds().height() > 0) {
            int trackX = bounds().right() - 4;
            UiPaint.rect(context, new Bounds(trackX, bounds().y(), 2, bounds().height()),
                    context.theme().palette().divider());
            int thumbHeight = scrollbarThumbHeight();
            int travel = bounds().height() - thumbHeight;
            int thumbY = bounds().y() + (travel == 0 ? 0
                    : (int) Math.round(travel * visualScrollOffset / maxScrollOffset()));
            boolean pointerOnScrollbar = bounds().contains(context.mouseX(), context.mouseY())
                    && context.mouseX() >= bounds().right() - 8;
            double shown = animate(context, "scrollbar-visible", pointerOnScrollbar || draggingScrollbar ? 1 : 0);
            int thumbColor = UiPaint.withAlpha(context.theme().palette().accent(), 0.35 + shown * 0.65);
            Bounds thumb=new Bounds(trackX-1,thumbY,4,thumbHeight);
            if(!context.theme().skin().surface(context,thumb,SurfaceVariant.RAISED,context.theme().palette().button()))UiPaint.roundedRect(context,thumb,thumbColor,context.theme().tokens().radii().pill());
        }
    }

    @Override
    public UiComponent hitTest(double mouseX, double mouseY) {
        if (!isEnabled() || !bounds().contains(mouseX, mouseY)) return null;
        if (maxScrollOffset() > 0 && mouseX >= bounds().right() - 8) return this;
        for (int index = children().size() - 1; index >= 0; index--) {
            UiComponent hit = children().get(index).hitTest(mouseX, mouseY);
            if (hit != null) return hit;
        }
        return null;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) return true;
        if (!isEnabled() || !bounds().contains(mouseX, mouseY) || maxScrollOffset() == 0) return false;
        UiTheme theme = lastTheme == null ? UiThemes.globalTheme() : lastTheme;
        int nextOffset = Math.max(0, Math.min(maxScrollOffset(),
                scrollOffset - (int) Math.round(scrollY * Math.max(8, theme.tokens().largeGap()))));
        if (nextOffset == scrollOffset) return true;
        scrollOffset = nextOffset;
        relayout();
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isEnabled() || button != 0 || !bounds().contains(mouseX, mouseY) || maxScrollOffset() == 0
                || mouseX < bounds().right() - 8) return false;
        draggingScrollbar = true;
        scrollToMouse(mouseY);
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!draggingScrollbar || button != 0) return false;
        scrollToMouse(mouseY);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!draggingScrollbar || button != 0) return false;
        draggingScrollbar = false;
        return true;
    }

    private int scrollbarThumbHeight() {
        return Math.min(bounds().height(), Math.max(8,
                bounds().height() * bounds().height() / Math.max(1, contentHeight)));
    }

    private void scrollToMouse(double mouseY) {
        int travel = bounds().height() - scrollbarThumbHeight();
        int relative = (int) Math.round(mouseY - bounds().y() - scrollbarThumbHeight() / 2.0);
        scrollOffset = travel <= 0 ? 0
                : (int) ((long) Math.max(0, Math.min(travel, relative)) * maxScrollOffset() / travel);
        relayout();
    }

    @Override
    public void visitFocusable(java.util.function.Consumer<UiComponent> visitor) {
        if (!isEnabled()) return;
        for (UiComponent child : children()) {
            if (child.bounds().bottom() > bounds().y() && child.bounds().y() < bounds().bottom()) {
                child.visitFocusable(visitor);
            }
        }
    }
}
