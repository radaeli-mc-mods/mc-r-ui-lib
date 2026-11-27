package com.radaeli.ruilib.ui;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

/** Single mutually exclusive selection. Arrow keys skip disabled options and commit immediately. */
public final class SegmentedControl<T> extends UiComponent {
  public record Option<T>(T value, Component label, boolean enabled) {
    public Option(T value, Component label) {
      this(value, label, true);
    }

    public Option {
      Objects.requireNonNull(value);
      Objects.requireNonNull(label);
    }
  }

  private final List<Option<T>> options;
  private final Consumer<T> onChange;
  private T value;
  private int pressed = -1;
  private boolean pixelated;
  @Override protected void layoutChildren(Font f,UiTheme t){pixelated=t.skin().pixelated();}

  public SegmentedControl(List<Option<T>> options, T value, Consumer<T> onChange) {
    this.options = List.copyOf(options);
    if (options.isEmpty()
        || options.stream().map(Option::value).distinct().count() != options.size())
      throw new IllegalArgumentException("Unique, nonempty options required");
    this.onChange = Objects.requireNonNull(onChange);
    setValue(value);
  }

  public T value() {
    return value;
  }

  public List<Option<T>> options() {
    return options;
  }

  public void setValue(T value) {
    if (options.stream().noneMatch(o -> o.value().equals(value)))
      throw new IllegalArgumentException("Unknown value");
    this.value = value;
  }

  @Override
  protected UiSize measureIntrinsic(Font font, UiTheme theme, int maxWidth, int maxHeight) {
    int cell =
        options.stream().mapToInt(o -> UiText.width(font, o.label(), theme) + 16).max().orElse(40);
    return new UiSize(cell * options.size(), Math.max(24, UiText.height(font, theme) + 10))
        .constrain(maxWidth, maxHeight);
  }

  private Bounds cell(int index) {
    int left = bounds().x() + bounds().width() * index / options.size();
    int right = bounds().x() + bounds().width() * (index + 1) / options.size();
    return new Bounds(left, bounds().y(), Math.max(0, right - left - (pixelated?1:2)), bounds().height());
  }

  @Override
  public void render(UiContext context) {
    for (int i = 0; i < options.size(); i++) {
      var option = options.get(i);
      var b = cell(i);
      boolean enabled = isEnabled() && option.enabled();
      boolean selected = option.value().equals(value),
          hover = enabled && b.contains(context.mouseX(), context.mouseY());
      UiPaint.chip(context, b, selected, hover, enabled, isFocused() && selected);
      UiPaint.text(
          context,
          option.label(),
          b.centerX() - UiText.width(context.font(), option.label(), context.theme()) / 2,
          b.centerY() - UiText.height(context) / 2,
          context.theme().skin().buttonText(context.theme(),enabled,selected));
    }
  }

  private int at(double x, double y) {
    for (int i = 0; i < options.size(); i++)
      if (cell(i).contains(x, y) && options.get(i).enabled()) return i;
    return -1;
  }

  private void choose(int index) {
    T next = options.get(index).value();
    if (!next.equals(value)) {
      value = next;
      onChange.accept(next);
    }
  }

  @Override
  public boolean isFocusable() {
    return true;
  }

  @Override
  public boolean mouseClicked(double x, double y, int button) {
    if (!isEnabled() || button != 0) return false;
    pressed = at(x, y);
    return pressed >= 0;
  }

  @Override
  public boolean mouseReleased(double x, double y, int button) {
    if (pressed < 0 || button != 0) return false;
    int old = pressed;
    pressed = -1;
    if (isEnabled() && at(x, y) == old) choose(old);
    return true;
  }

  @Override
  public boolean keyPressed(int key, int scan, int modifiers) {
    if (!isEnabled() || !isFocused()) return false;
    if (key != 262 && key != 263 && key != 268 && key != 269 && key != 32 && key != 257)
      return false;
    if (key == 32 || key == 257) return true;
    int current = 0;
    for (int i = 0; i < options.size(); i++) if (options.get(i).value().equals(value)) current = i;
    int direction = key == 263 || key == 269 ? -1 : 1;
    int start = key == 268 ? options.size() - 1 : key == 269 ? 0 : current;
    for (int step = 1; step <= options.size(); step++) {
      int index = Math.floorMod(start + direction * step, options.size());
      if (options.get(index).enabled()) {
        choose(index);
        break;
      }
    }
    return true;
  }
}
