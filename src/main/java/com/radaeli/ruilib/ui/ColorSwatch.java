package com.radaeli.ruilib.ui;

import java.util.Objects;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

/** Opaque RGB swatch, selectable by mouse, Enter or Space. */
public final class ColorSwatch extends UiComponent {
  private int rgb;
  private boolean selected;
  private final Runnable onSelect;

  public ColorSwatch(int rgb, boolean selected, Runnable onSelect) {
    this.rgb = rgb & 0xffffff;
    this.selected = selected;
    this.onSelect = Objects.requireNonNull(onSelect);
  }

  public int rgb() {
    return rgb;
  }

  public void setColor(int rgb) {
    this.rgb = rgb & 0xffffff;
  }

  public void setSelected(boolean value) {
    selected = value;
  }

  @Override
  public Tooltip tooltip() {
    return super.tooltip() != null ? super.tooltip() : Tooltip.of(Component.literal(String.format("#%06X", rgb)));
  }

  @Override
  protected UiSize measureIntrinsic(Font f, UiTheme t, int w, int h) {
    return new UiSize(24, 24).constrain(w, h);
  }

  @Override
  public void render(UiContext c) {
    if(c.theme().skin().pixelated()){
      c.theme().skin().surface(c,bounds(),SurfaceVariant.INSET,0xff000000|rgb);
      if(selected||isFocused())UiPaint.outline(c,bounds(),isFocused()?c.theme().palette().focus():c.theme().palette().accent());
      if(!isEnabled())UiPaint.outline(c,bounds().inset(Insets.all(1)),c.theme().palette().disabled());return;
    }
    UiPaint.rect(
        c,
        bounds(),
        selected || isFocused() ? c.theme().palette().accent() : c.theme().palette().divider());
    UiPaint.rect(
        c,
        new Bounds(
            bounds().x() + 2,
            bounds().y() + 2,
            Math.max(0, bounds().width() - 4),
            Math.max(0, bounds().height() - 4)),
        0xff000000 | rgb);
  }

  @Override
  public boolean isFocusable() {
    return true;
  }

  @Override
  public boolean mouseClicked(double x, double y, int button) {
    if (!isEnabled() || button != 0 || !bounds().contains(x, y)) return false;
    onSelect.run();
    return true;
  }

  @Override
  public boolean keyPressed(int key, int scan, int mods) {
    if (isEnabled() && isFocused() && (key == 257 || key == 335 || key == 32)) {
      onSelect.run();
      return true;
    }
    return false;
  }
}
