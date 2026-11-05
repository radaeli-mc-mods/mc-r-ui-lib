package com.radaeli.ruilib.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Vertical list of {@link Radio} rows with one exclusive value, which may start empty. Mouse commits on
 * release; Up/Down move and commit immediately, skipping disabled options; Home/End pick the first/last
 * enabled option.
 */
public final class RadioGroup<T> extends UiComponent {
    public record Option<T>(T value, Component label, Component hint, boolean enabled) {
        public Option(T value, Component label) {
            this(value, label, Component.empty(), true);
        }

        public Option(T value, Component label, boolean enabled) {
            this(value, label, Component.empty(), enabled);
        }

        public Option {
            Objects.requireNonNull(value);
            Objects.requireNonNull(label);
            Objects.requireNonNull(hint);
        }
    }

    private final List<Option<T>> options;
    private final Consumer<T> onChange;
    private final int gap;
    private T value;
    private int pressed = -1;

    public RadioGroup(List<Option<T>> options, T value, Consumer<T> onChange) {
        this(options, value, 0, onChange);
    }

    public RadioGroup(List<Option<T>> options, T value, int gap, Consumer<T> onChange) {
        this.options = List.copyOf(options);
        if (options.isEmpty()
                || options.stream().map(Option::value).distinct().count() != options.size())
            throw new IllegalArgumentException("Unique, nonempty options required");
        this.onChange = Objects.requireNonNull(onChange);
        this.gap = Math.max(0, gap);
        setValue(value);
    }

    /** The selected value, or {@code null} while nothing is selected. */
    public T value() {
        return value;
    }

    public List<Option<T>> options() {
        return options;
    }

    /** Updates the selection without invoking the callback; {@code null} clears it. */
    public void setValue(T value) {
        if (value != null && options.stream().noneMatch(o -> o.value().equals(value)))
            throw new IllegalArgumentException("Unknown value");
        this.value = value;
    }

    private int rowHeight(Font font, UiTheme theme) {
        return Radio.rowSize(font, theme, Component.empty(), Component.empty()).height();
    }

    @Override
    protected UiSize measureIntrinsic(Font font, UiTheme theme, int maxWidth, int maxHeight) {
        int width = options.stream().mapToInt(o -> Radio.rowSize(font, theme, o.label(), o.hint()).width())
                .max().orElse(40);
        int height = rowHeight(font, theme) * options.size() + gap * (options.size() - 1);
        return new UiSize(width, height).constrain(maxWidth, maxHeight);
    }

    private Bounds row(int index) {
        int top = bounds().y() + bounds().height() * index / options.size();
        int bottom = bounds().y() + bounds().height() * (index + 1) / options.size();
        return new Bounds(bounds().x(), top, bounds().width(), Math.max(0, bottom - top - (index < options.size() - 1 ? gap : 0)));
    }

    @Override
    public void render(UiContext context) {
        for (int i = 0; i < options.size(); i++) {
            var option = options.get(i);
            var b = row(i);
            boolean enabled = isEnabled() && option.enabled();
            boolean selected = option.value().equals(value);
            boolean hover = enabled && b.contains(context.mouseX(), context.mouseY());
            Radio.paintRow(context, b, option.label(), option.hint(),
                    animate(context, "selected" + i, selected ? 1 : 0), animate(context, "hover" + i, hover ? 1 : 0),
                    enabled, animate(context, "focus" + i, isFocused() && (selected || value == null && i == firstEnabled()) ? 1 : 0));
        }
    }

    private int at(double x, double y) {
        for (int i = 0; i < options.size(); i++)
            if (row(i).contains(x, y) && options.get(i).enabled()) return i;
        return -1;
    }

    private int firstEnabled() {
        for (int i = 0; i < options.size(); i++) if (options.get(i).enabled()) return i;
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
        return firstEnabled() >= 0;
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (!isEnabled() || button != 0) return false;
        pressed = at(x, y);
        if (pressed >= 0) setFocused(true);
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
        if (key != 264 && key != 265 && key != 268 && key != 269 && key != 32 && key != 257 && key != 335)
            return false;
        int current = -1;
        for (int i = 0; i < options.size(); i++) if (options.get(i).value().equals(value)) current = i;
        if (key == 32 || key == 257 || key == 335) {
            if (current < 0 && firstEnabled() >= 0) choose(firstEnabled());
            return true;
        }
        int direction = key == 265 || key == 269 ? -1 : 1;
        int start = key == 268 ? options.size() - 1 : key == 269 ? 0 : current < 0 ? (direction > 0 ? -1 : 0) : current;
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
