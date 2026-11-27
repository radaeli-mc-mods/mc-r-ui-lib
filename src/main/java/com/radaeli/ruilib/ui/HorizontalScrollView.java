package com.radaeli.ruilib.ui;

import java.util.function.Consumer;
import net.minecraft.client.gui.Font;

/**
 * Horizontal viewport for fixed-width children. Wheel, draggable track and arrow keys scroll it.
 */
public final class HorizontalScrollView extends UiContainer {
  private final int gap;
  private int offset, extent;
  private Font font;
  private UiTheme theme;
  private boolean dragging;

  public HorizontalScrollView(int gap) {
    if (gap < 0) throw new IllegalArgumentException("Negative gap");
    this.gap = gap;
  }

  public int scrollOffset() {
    return offset;
  }

  public int maxScrollOffset() {
    return Math.max(0, extent - bounds().width());
  }

  public void setScrollOffset(int offset) {
    this.offset = Math.max(0, Math.min(offset, maxScrollOffset()));
    if (font != null) layoutChildren(font, theme);
  }

  @Override
  protected UiSize measureIntrinsic(Font f, UiTheme t, int w, int h) {
    return measureChildren(f, t, w, h, gap, true);
  }

  @Override
  protected void layoutChildren(Font f, UiTheme t) {
    font = f;
    theme = t;
    extent = 0;
    var sizes = new UiSize[children().size()];
    int h = Math.max(0, bounds().height() - 10);
    for (int i = 0; i < sizes.length; i++) {
      sizes[i] = children().get(i).measure(f, t, 8192, h);
      extent += sizes[i].width() + (i > 0 ? gap : 0);
    }
    offset = Math.min(offset, maxScrollOffset());
    int x = bounds().x() - offset;
    for (int i = 0; i < sizes.length; i++) {
      children().get(i).layout(f, t, new Bounds(x, bounds().y(), sizes[i].width(), h));
      x += sizes[i].width() + gap;
    }
  }

  @Override
  public void render(UiContext c) {
    if(c.theme().skin().pixelated())c.theme().skin().surface(c,bounds(),SurfaceVariant.INSET,WorkshopSkin.WELL);
    c.graphics().enableScissor(bounds().x(), bounds().y(), bounds().right(), bounds().bottom());
    try {
      super.render(c);
    } finally {
      c.graphics().disableScissor();
    }
    if (maxScrollOffset() > 0) {
      UiPaint.rect(
          c,
          new Bounds(bounds().x(), bounds().bottom() - 6, bounds().width(), 4),
          c.theme().palette().divider());
      int thumb = thumb();
      int x = bounds().x() + (int) ((long) offset * (bounds().width() - thumb) / maxScrollOffset());
      Bounds handle=new Bounds(x,bounds().bottom()-6,thumb,4);
      if(!c.theme().skin().surface(c,handle,SurfaceVariant.RAISED,c.theme().palette().button()))UiPaint.rect(c,handle,c.theme().palette().accent());
    }
  }

  private int thumb() {
    return Math.min(
        bounds().width(), Math.max(12, bounds().width() * bounds().width() / Math.max(1, extent)));
  }

  @Override
  public UiComponent hitTest(double x, double y) {
    if (!isEnabled() || !bounds().contains(x, y)) return null;
    if (y >= bounds().bottom() - 10) return this;
    var hit = super.hitTest(x, y);
    return hit == null ? this : hit;
  }

  @Override
  public boolean mouseScrolled(double x, double y, double sx, double sy) {
    if (!isEnabled() || !bounds().contains(x, y)) return false;
    if (super.mouseScrolled(x, y, sx, sy)) return true;
    setScrollOffset(offset - (int) Math.round((sx != 0 ? sx : sy) * 32));
    return maxScrollOffset() > 0;
  }

  @Override
  public boolean isFocusable() {
    return true;
  }

  @Override
  public boolean keyPressed(int key, int scan, int mods) {
    if (!isEnabled() || !isFocused()) return false;
    if (key == 263 || key == 262) {
      setScrollOffset(offset + (key == 263 ? -48 : 48));
      return true;
    }
    return false;
  }

  @Override
  public boolean mouseClicked(double x, double y, int button) {
    if (button != 0 || !isEnabled() || !bounds().contains(x, y) || y < bounds().bottom() - 10)
      return false;
    dragging = true;
    drag(x);
    return true;
  }

  private void drag(double x) {
    int travel = bounds().width() - thumb();
    setScrollOffset(
        travel <= 0 ? 0 : (int) ((x - bounds().x() - thumb() / 2.0) * maxScrollOffset() / travel));
  }

  @Override
  public boolean mouseDragged(double x, double y, int b, double dx, double dy) {
    if (!dragging || b != 0) return false;
    drag(x);
    return true;
  }

  @Override
  public boolean mouseReleased(double x, double y, int b) {
    if (b != 0 || !dragging) return false;
    dragging = false;
    return true;
  }

  @Override
  public void visitFocusable(Consumer<UiComponent> visitor) {
    if (!isEnabled()) return;
    visitor.accept(this);
    for (var child : children())
      if (child.bounds().right() > bounds().x() && child.bounds().x() < bounds().right())
        child.visitFocusable(visitor);
  }
}
