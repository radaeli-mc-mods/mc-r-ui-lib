package com.radaeli.ruilib.ui;

import java.util.function.IntConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

/**
 * Transactional RGB editor: visual RGB sliders and hex input share a preview; Apply alone commits.
 */
public final class ColorPicker extends UiContainer {
  private final Column content = new Column(4);
  private final TextField hex = new TextField(Component.literal("#RRGGBB"));
  private final ColorSwatch preview;
  private final Slider[] channels = new Slider[3];
  private final Button apply;
  private int value;
  private boolean syncing;

  public ColorPicker(int initial, IntConsumer onApply, Runnable onCancel) {
    value = initial & 0xffffff;
    preview = new ColorSwatch(value, false, () -> {});
    preview.setPreferredSize(null, 28);
    content.add(preview);
    hex.setMaxLength(7);
    hex.setValue(String.format("#%06X", value));
    content.add(hex);
    for (int i = 0; i < 3; i++) {
      final int shift = (2 - i) * 8;
      content.add(new Label(Component.literal(new String[] {"Red", "Green", "Blue"}[i])));
      channels[i] =
          content.add(
              new Slider(
                  0,
                  255,
                  value >> shift & 255,
                  1,
                  v -> {
                    if (!syncing) {
                      value = (value & ~(255 << shift)) | ((int) v << shift);
                      sync();
                    }
                  }));
    }
    Row actions = content.add(new Row(4));
    apply = actions.add(new Button(Component.literal("Apply"), () -> onApply.accept(value)));
    actions.add(new Button(Component.literal("Cancel"), onCancel));
    hex.setResponder(
        text -> {
          if (syncing) return;
          boolean valid = text.matches("#[0-9a-fA-F]{6}");
          apply.setEnabled(valid);
          hex.setInvalid(!valid);
          if (valid) {
            value = Integer.parseInt(text.substring(1), 16);
            sync();
          }
        });
    add(content);
  }

  public int value() {
    return value;
  }

  private void sync() {
    syncing = true;
    try {
      preview.setColor(value);
      hex.setValue(String.format("#%06X", value));
      for (int i = 0; i < 3; i++) channels[i].setValue(value >> ((2 - i) * 8) & 255);
      apply.setEnabled(true);
    } finally {
      syncing = false;
    }
  }

  @Override
  protected UiSize measureIntrinsic(Font f, UiTheme t, int w, int h) {
    return content.measure(f, t, w, h);
  }

  @Override
  protected void layoutChildren(Font f, UiTheme t) {
    content.layout(f, t, bounds());
  }
}
