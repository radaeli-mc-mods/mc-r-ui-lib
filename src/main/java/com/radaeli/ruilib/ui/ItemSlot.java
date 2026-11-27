package com.radaeli.ruilib.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * A ghost item slot for filters and other item references. It never inserts an item into an inventory.
 */
public final class ItemSlot extends UiComponent {
    private static final int DEFAULT_SIZE = 24;
    private static final int ICON_SIZE = 16;

    private ItemStack itemStack = ItemStack.EMPTY;
    private UiColor backgroundColor;
    private Consumer<ItemStack> onItemChanged = ignored -> { };
    private Consumer<ItemStack> onDrop = ignored -> { };
    private boolean pressed;
    private boolean dropTargetActive;

    public ItemSlot() {
    }

    public ItemSlot(ItemStack itemStack) {
        this.itemStack = copyAsGhost(Objects.requireNonNull(itemStack, "itemStack"));
    }

    public ItemSlot(ItemStack itemStack, Consumer<ItemStack> onItemChanged) {
        this.itemStack = copyAsGhost(Objects.requireNonNull(itemStack, "itemStack"));
        this.onItemChanged = Objects.requireNonNull(onItemChanged, "onItemChanged");
    }

    /** Returns a defensive copy of the selected ghost item, or {@link ItemStack#EMPTY}. */
    public ItemStack itemStack() {
        return itemStack.copy();
    }

    /** Sets the selected item reference. Non-empty stacks are normalized to count one. */
    public ItemSlot setItemStack(ItemStack itemStack) {
        Objects.requireNonNull(itemStack, "itemStack");
        ItemStack next = copyAsGhost(itemStack);
        if (!ItemStack.matches(this.itemStack, next)) {
            this.itemStack = next;
            onItemChanged.accept(next.copy());
        }
        return this;
    }

    private static ItemStack copyAsGhost(ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack copy = stack.copy();
        copy.setCount(1);
        return copy;
    }

    /** Clears the selected ghost item and reports {@link ItemStack#EMPTY} to the consumer. */
    public void clear() {
        setItemStack(ItemStack.EMPTY);
    }

    /** Replaces the callback invoked when the selected ghost item changes. */
    public ItemSlot setOnItemChanged(Consumer<ItemStack> onItemChanged) {
        this.onItemChanged = Objects.requireNonNull(onItemChanged, "onItemChanged");
        return this;
    }

    /** Invoked whenever an inventory picker item is dropped here, even if it matches the current item. */
    public ItemSlot setOnDrop(Consumer<ItemStack> onDrop) {
        this.onDrop = Objects.requireNonNull(onDrop, "onDrop");
        return this;
    }

    void acceptDrop(ItemStack itemStack) {
        setItemStack(itemStack);
        onDrop.accept(itemStack.copy());
    }

    /** Overrides the theme background color; an empty value keeps the active theme default. */
    public Optional<UiColor> backgroundColor() {
        return Optional.ofNullable(backgroundColor);
    }

    public ItemSlot setBackgroundColor(UiColor backgroundColor) {
        this.backgroundColor = Objects.requireNonNull(backgroundColor, "backgroundColor");
        return this;
    }

    /** Sets a custom ARGB background color. */
    public ItemSlot setBackgroundColor(int argb) {
        return setBackgroundColor(new UiColor(argb));
    }

    /** Removes the local override and returns to the active theme's input background. */
    public ItemSlot clearBackgroundColor() {
        backgroundColor = null;
        return this;
    }

    /** Allows a custom drag integration to show that this slot can accept the dragged item. */
    public ItemSlot setDropTargetActive(boolean active) {
        dropTargetActive = active;
        return this;
    }

    public boolean isDropTargetActive() {
        return dropTargetActive;
    }

    @Override
    protected UiSize measureIntrinsic(Font font, UiTheme theme, int maxWidth, int maxHeight) {
        return new UiSize(DEFAULT_SIZE, DEFAULT_SIZE).constrain(maxWidth, maxHeight);
    }

    @Override
    public void render(UiContext context) {
        boolean hovered = isEnabled() && bounds().contains(context.mouseX(), context.mouseY());
        double hover = animate(context, "hover", hovered ? 1 : 0);
        double focused = animate(context, "focus", isFocused() ? 1 : 0);
        double pressedAmount = animate(context, "press", pressed ? 1 : 0);
        UiPaint.input(context, bounds(), hover, isEnabled(), focused,
                backgroundColor == null ? (context.theme().skin().pixelated()?WorkshopSkin.WELL:null) : backgroundColor.argb());
        if (dropTargetActive) {
            UiPaint.roundedOutline(context, bounds(), context.theme().palette().accent(),
                    context.theme().tokens().radii().medium());
        }
        if (!itemStack.isEmpty()) {
            ItemRenderer.render(context, itemStack, bounds().centerX() - ICON_SIZE / 2,
                    bounds().centerY() - ICON_SIZE / 2);
            if (hovered) ItemRenderer.renderTooltip(context, itemStack);
        } else {
            int color = isEnabled() ? context.theme().palette().mutedText()
                    : context.theme().palette().disabled();
            UiPaint.textCentered(context, "+", bounds().centerX(),
                    bounds().centerY() - UiText.height(context) / 2, color);
        }
        if (pressedAmount > 0) {
            UiPaint.roundedOutline(context, bounds(),
                    UiPaint.withAlpha(context.theme().palette().accent(), pressedAmount),
                    context.theme().tokens().radii().medium());
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isEnabled() || !bounds().contains(mouseX, mouseY) || (button != 0 && button != 1)) return false;
        setFocused(true);
        pressed = true;
        if (button == 1) clear();
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!pressed || (button != 0 && button != 1)) return false;
        pressed = false;
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!isEnabled() || !isFocused()) return false;
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE) {
            clear();
            return true;
        }
        return false;
    }

    @Override
    public boolean isFocusable() {
        return true;
    }
}
