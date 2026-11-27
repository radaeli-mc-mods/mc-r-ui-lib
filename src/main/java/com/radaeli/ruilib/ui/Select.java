package com.radaeli.ruilib.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/** Single-value dropdown. UiScreen hosts the popup outside parent clipping bounds. */
public final class Select<T> extends UiComponent {
    public record Option<T>(T value, Component label, Tooltip tooltip) {
        public Option(T value, Component label) { this(value, label, null); }
        public Option {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(label, "label");
        }
    }

    private final List<Option<T>> options;
    private final Consumer<T> onChange;
    private int selectedIndex;
    private int highlightedIndex;
    private int firstVisible;
    private int visibleRows;
    private int rowHeight;
    private int popupInset=1;
    private boolean open;
    private boolean pressed;
    private Bounds popupBounds = Bounds.EMPTY;

    public Select(List<Option<T>> options, T value, Consumer<T> onChange) {
        this.options = List.copyOf(options);
        if (options.isEmpty()) throw new IllegalArgumentException("Select requires at least one option");
        this.onChange = Objects.requireNonNull(onChange, "onChange");
        setValue(value);
    }

    public List<Option<T>> options() { return options; }
    public T value() { return options.get(selectedIndex).value(); }
    public boolean isOpen() { return open; }

    /** Updates the selected value without notifying the consumer. */
    public void setValue(T value) {
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).value().equals(value)) {
                selectedIndex = highlightedIndex = i;
                return;
            }
        }
        throw new IllegalArgumentException("Value is not a Select option: " + value);
    }

    public void close() { open = false; popupBounds = Bounds.EMPTY; }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (!isFocused()) { close(); pressed = false; }
    }

    @Override
    public Tooltip tooltip() {
        Tooltip selected = options.get(selectedIndex).tooltip();
        return selected == null ? super.tooltip() : selected;
    }

    @Override
    protected UiSize measureIntrinsic(Font font, UiTheme theme, int maxWidth, int maxHeight) {
        int widest = options.stream().mapToInt(option -> UiText.width(font, option.label(), theme)).max().orElse(0);
        Insets padding = theme.tokens().buttonPadding();
        return new UiSize(Math.max(theme.tokens().buttonMinimumWidth(), widest + padding.left() + padding.right() + 16),
                Math.max(theme.tokens().buttonMinimumHeight(), UiText.height(font, theme) + padding.top() + padding.bottom()))
                .constrain(maxWidth, maxHeight);
    }

    @Override
    public void render(UiContext context) {
        boolean hovered = bounds().contains(context.mouseX(), context.mouseY());
        UiPaint.button(context, bounds(), hovered && isEnabled(), pressed || open, isEnabled(), isFocused());
        int color = context.theme().skin().buttonText(context.theme(),isEnabled(),false);
        drawLabel(context, options.get(selectedIndex).label(), bounds(), 20, color);
        if(context.theme().skin().pixelated()) { (open?UiIcons.UP:UiIcons.DOWN).render(context,bounds().right()-12,bounds().centerY()-2,color);return; }
        UiPaint.textCentered(context, Component.literal(open ? "^" : "v"), bounds().right() - 9,
                bounds().centerY() - UiText.height(context) / 2, color);
    }

    private static void drawLabel(UiContext context, Component label, Bounds area, int rightInset, int color) {
        int available = Math.max(0,area.width()-8-rightInset);
        UiPaint.text(context,UiText.ellipsize(context.font(),label,(int)(available/context.theme().tokens().typography().bodyScale())),
            area.x()+8,area.centerY()-UiText.height(context)/2,color,UiTextStyle.BODY);
    }

    void layoutPopup(Font font, UiTheme theme, int screenWidth, int screenHeight) {
        popupInset=theme.skin().pixelated()?theme.skin().metrics().frameInset():1;
        rowHeight = Math.max(theme.skin().metrics().popupRowHeight(), UiText.height(font, theme) + (theme.skin().pixelated()?6:10));
        int desiredHeight = Math.min(8, options.size()) * rowHeight + popupInset*2;
        int below = Math.max(0, screenHeight - bounds().bottom() - 6);
        int above = Math.max(0, bounds().y() - 6);
        boolean upwards = below < desiredHeight && above > below;
        visibleRows = Math.min(Math.min(8, options.size()), Math.max(0, ((upwards ? above : below) - popupInset*2) / rowHeight));
        if (visibleRows == 0) { close(); return; }
        int widest = options.stream().mapToInt(option -> UiText.width(font, option.label(), theme)).max().orElse(0);
        int width = Math.min(Math.max(0, screenWidth - 8), Math.max(bounds().width(), widest + 24));
        int x = Math.max(4, Math.min(bounds().x(), screenWidth - width - 4));
        int height = visibleRows * rowHeight + popupInset*2;
        int y = upwards ? bounds().y() - height - 2 : bounds().bottom() + 2;
        popupBounds = new Bounds(x, y, width, height);
        firstVisible = Math.max(0, Math.min(firstVisible, options.size() - visibleRows));
        revealHighlight();
    }

    private void revealHighlight() {
        if (visibleRows == 0) return;
        if (highlightedIndex < firstVisible) firstVisible = highlightedIndex;
        else if (highlightedIndex >= firstVisible + visibleRows) firstVisible = highlightedIndex - visibleRows + 1;
    }

    public void renderPopup(UiContext context) {
        if (!open) return;
        UiPaint.card(context, popupBounds);
        int hovered = optionAt(context.mouseX(), context.mouseY());
        for (int row = 0; row < visibleRows; row++) {
            int index = firstVisible + row;
            Bounds area = new Bounds(popupBounds.x() + popupInset, popupBounds.y() + popupInset + row * rowHeight,
                    Math.max(0, popupBounds.width() - popupInset*2), rowHeight);
            if(context.theme().skin().pixelated())UiPaint.rect(context,area,index==selectedIndex?context.theme().palette().buttonPressed():index==hovered||index==highlightedIndex?context.theme().palette().buttonHover():WorkshopSkin.ROW);
            else if (index == hovered || index == highlightedIndex) UiPaint.rect(context, area, context.theme().palette().buttonHover());
            if (index == selectedIndex) UiPaint.rect(context, new Bounds(area.x(), area.y(), 2, area.height()),
                    context.theme().palette().accent());
            drawLabel(context, options.get(index).label(), area, 8, context.theme().skin().pixelated()&&index==selectedIndex?WorkshopSkin.TEXT_SELECTED:context.theme().palette().text());
        }
    }

    public boolean popupContains(double x, double y) { return open && popupBounds.contains(x, y); }

    private int optionAt(double x, double y) {
        if (!popupContains(x, y) || y < popupBounds.y() + popupInset || y >= popupBounds.bottom() - popupInset) return -1;
        return firstVisible + (int) (y - popupBounds.y() - popupInset) / rowHeight;
    }

    public Tooltip popupTooltip(double x, double y) {
        int index = optionAt(x, y);
        return index < 0 ? null : options.get(index).tooltip();
    }

    void chooseAt(double x, double y) {
        int index = optionAt(x, y);
        if (index >= 0) choose(index);
    }

    private void choose(int index) {
        boolean changed = selectedIndex != index;
        selectedIndex = highlightedIndex = index;
        close();
        if (changed) onChange.accept(value());
    }

    private void toggleOpen() {
        if (open) close();
        else {
            open = true;
            highlightedIndex = selectedIndex;
            firstVisible = selectedIndex;
        }
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (!isEnabled() || button != 0 || !bounds().contains(x, y)) return false;
        pressed = true;
        setFocused(true);
        return true;
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        if (!pressed || button != 0) return false;
        pressed = false;
        if (isEnabled() && bounds().contains(x, y)) toggleOpen();
        return true;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (!isEnabled() || !popupContains(x, y) || scrollY == 0) return false;
        int direction = scrollY > 0 ? -1 : 1;
        highlightedIndex = Math.max(0, Math.min(options.size() - 1, highlightedIndex + direction));
        revealHighlight();
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!isEnabled() || !isFocused()) return false;
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && open) { close(); return true; }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_SPACE) {
            if (open) choose(highlightedIndex); else toggleOpen();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DOWN || keyCode == GLFW.GLFW_KEY_UP
                || keyCode == GLFW.GLFW_KEY_HOME || keyCode == GLFW.GLFW_KEY_END) {
            if (!open) toggleOpen();
            if (keyCode == GLFW.GLFW_KEY_HOME) highlightedIndex = 0;
            else if (keyCode == GLFW.GLFW_KEY_END) highlightedIndex = options.size() - 1;
            else highlightedIndex = Math.max(0, Math.min(options.size() - 1,
                        highlightedIndex + (keyCode == GLFW.GLFW_KEY_DOWN ? 1 : -1)));
            revealHighlight();
            return true;
        }
        return false;
    }

    @Override
    public boolean isFocusable() { return true; }
}
