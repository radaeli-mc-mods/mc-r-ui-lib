package com.radaeli.ruilib.ui;

import java.util.Objects;
import java.util.function.Consumer;

/** Selectable, neutral frame. Interactive descendants receive input without bubbling selection. */
public final class SelectableCard extends UiFrame {
  private final Runnable onSelect;
  private boolean selected, pressed;

  public SelectableCard(UiComponent content, boolean selected, Runnable onSelect) {
    super(content, Insets.all(8));
    this.selected = selected;
    this.onSelect = Objects.requireNonNull(onSelect);
  }

  public boolean isSelected() {
    return selected;
  }

  public void setSelected(boolean selected) {
    this.selected = selected;
  }

  public UiComponent contentComponent() {
    return content();
  }

  @Override
  public void render(UiContext context) {
    if(context.theme().skin().pixelated()){
      context.theme().skin().button(context,bounds(),new UiVisualState(isEnabled(),bounds().contains(context.mouseX(),context.mouseY())?1:0,pressed?1:0,selected?1:0,isFocused()?1:0));
      if(selected)UiPaint.rect(context,new Bounds(bounds().x()+3,bounds().y()+3,2,Math.max(0,bounds().height()-6)),context.theme().palette().accent());
      content().render(selected?context.selectedSurface():context);return;
    }
    UiPaint.panel(context, bounds());
    boolean hover = isEnabled() && bounds().contains(context.mouseX(), context.mouseY());
    UiPaint.outline(
        context,
        bounds(),
        selected
            ? context.theme().palette().accent()
            : hover ? context.theme().palette().mutedText() : context.theme().palette().divider());
    if (isFocused())
      UiPaint.outline(
          context,
          new Bounds(
              bounds().x() + 2,
              bounds().y() + 2,
              Math.max(0, bounds().width() - 4),
              Math.max(0, bounds().height() - 4)),
          context.theme().palette().text());
    content().render(context);
  }

  @Override
  public UiComponent hitTest(double x, double y) {
    if (!isEnabled() || !bounds().contains(x, y)) return null;
    UiComponent hit = content().hitTest(x, y);
    return hit != null && hit.isFocusable() ? hit : this;
  }

  @Override
  public boolean isFocusable() {
    return true;
  }

  @Override
  public void visitFocusable(Consumer<UiComponent> visitor) {
    if (isEnabled()) {
      visitor.accept(this);
      content().visitFocusable(visitor);
    }
  }

  @Override
  public boolean mouseClicked(double x, double y, int button) {
    if (!isEnabled() || button != 0 || !bounds().contains(x, y)) return false;
    pressed = true;
    return true;
  }

  @Override
  public boolean mouseReleased(double x, double y, int button) {
    if (!pressed || button != 0) return false;
    pressed = false;
    if (isEnabled() && bounds().contains(x, y)) onSelect.run();
    return true;
  }

  @Override
  public boolean keyPressed(int key, int scan, int modifiers) {
    if (!isEnabled() || !isFocused() || key != 257 && key != 335 && key != 32) return false;
    onSelect.run();
    return true;
  }
}
