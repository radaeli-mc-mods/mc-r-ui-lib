package com.radaeli.ruilib.ui;

import net.minecraft.client.gui.GuiGraphics;
import com.radaeli.ruilib.platform.UiPlatformScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Thin bridge from a component tree to Minecraft's client Screen lifecycle. */
public class UiScreen extends UiPlatformScreen {
    private static final float CARRIED_ITEM_Z_INDEX = 450.0F;
    private final UiComponent root;
    private final UiTheme themeOverride;
    private UiTheme layoutTheme;
    private final ToastHost toastHost = new ToastHost();
    private UiComponent focusedComponent;
    private UiComponent pressedComponent;
    private Modal modal;
    private Modal closingModal;
    private ItemStack carriedItem = ItemStack.EMPTY;
    private InventoryGrid carriedFromGrid;
    private int carriedFromSlot = -1;
    private int carriedMouseButton = -1;
    private boolean draggingInventoryItem;

    public UiScreen(Component title, UiComponent root) {
        this(title, root, null);
    }

    public UiScreen(Component title, UiComponent root, UiTheme theme) {
        super(title);
        this.root = Objects.requireNonNull(root, "root");
        this.themeOverride = theme;
    }

    @Override
    protected void init() {
        UiTheme activeTheme = theme();
        toastHost.layout(font, activeTheme, new Bounds(0, 0, width, height));
        layoutRoot(activeTheme);
        if (modal != null) modal.layoutInScreen(font, activeTheme, width, height);
        if (closingModal != null) closingModal.layoutInScreen(font, activeTheme, width, height);
        layoutTheme = activeTheme;
        clearToolkitFocus();
    }

    @Override
    public void tick() {
        super.tick();
        toastHost.tick();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        ensureThemeLayout();
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        UiContext context = new UiContext(graphics, font, theme(), mouseX, mouseY, width, height);
        UiPaint.rect(context, new Bounds(0, 0, width, height), context.theme().palette().screen());
        root.render(context);
        renderToastOverlay(graphics, mouseX, mouseY);
        renderables.forEach(renderable -> renderable.render(graphics, mouseX, mouseY, partialTick));
        renderClosingModal(context);
        if (modal != null) modal.render(context);
        Select<?> select = openSelect();
        if (select != null) {
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 400);
            select.renderPopup(context);
            graphics.pose().popPose();
        }
        renderCarriedItem(graphics, mouseX, mouseY);
        if (select != null && select.popupContains(mouseX, mouseY)) {
            Tooltip tooltip = select.popupTooltip(mouseX, mouseY);
            if (tooltip != null) UiPaint.tooltip(context, tooltip, mouseX, mouseY);
            return;
        }
        UiComponent hovered = closingModal == null ? activeRoot().hitTest(mouseX, mouseY) : null;
        if (hovered instanceof ItemPreview preview) preview.renderTooltip(context);
        Tooltip hoveredTooltip = hovered == null ? null : hovered.tooltipAt(mouseX, mouseY);
        if (hoveredTooltip != null) {
            UiPaint.tooltip(context, hoveredTooltip, mouseX, mouseY);
        }
    }

    protected void renderToastOverlay(GuiGraphics graphics, int mouseX, int mouseY) {
        toastHost.render(new UiContext(graphics, font, theme(), mouseX, mouseY, width, height));
    }

    protected void layoutRoot(UiTheme theme) {
        root.layout(font, theme, new Bounds(0, 0, width, height));
    }

    protected void renderClosingModal(UiContext context) {
        if (closingModal == null) return;
        closingModal.render(context);
        if (closingModal.closeTransitionComplete()) {
            closingModal.clearContentFocus();
            closingModal = null;
        }
    }

    protected boolean handleToastClick(double mouseX, double mouseY, int button) {
        if (!toastHost.mouseClicked(mouseX, mouseY, button)) return false;
        clearToolkitFocus();
        pressedComponent = null;
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        ensureThemeLayout();
        Select<?> select = openSelect();
        if (select != null) {
            pressedComponent = null;
            if (select.bounds().contains(mouseX, mouseY)) {
                if (select.mouseClicked(mouseX, mouseY, button)) pressedComponent = select;
            } else if (select.popupContains(mouseX, mouseY)) {
                if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) select.chooseAt(mouseX, mouseY);
            } else select.close();
            return true;
        }
        if (modal != null) {
            if (modal.shouldCloseFromOutsideClick(mouseX, mouseY)) {
                closeModal();
                return true;
            }
            UiComponent target = modal.hitTest(mouseX, mouseY);
            if (handleInventoryPickerClick(target, mouseX, mouseY, button)) return true;
            clearToolkitFocus();
            if (target != null && target != modal && target.isFocusable()) focus(target);
            pressedComponent = target != null && target.mouseClicked(mouseX, mouseY, button) ? target : null;
            return true;
        }
        if (closingModal != null) return true;
        if (handleToastClick(mouseX, mouseY, button)) return true;
        UiComponent target = root.hitTest(mouseX, mouseY);
        if (handleInventoryPickerClick(target, mouseX, mouseY, button)) return true;
        clearToolkitFocus();
        if (target == null) {
            pressedComponent = null;
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (target.isFocusable()) {
            focus(target);
        }
        pressedComponent = target.mouseClicked(mouseX, mouseY, button) ? target : null;
        return pressedComponent != null || target.isFocusable();
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (closingModal != null) return true;
        if (draggingInventoryItem && button == carriedMouseButton) {
            UiComponent target = activeRoot().hitTest(mouseX, mouseY);
            if (target instanceof ItemSlot slot && slot.isEnabled() && !carriedItem.isEmpty()) {
                slot.acceptDrop(carriedItem);
                clearCarriedItem();
            }
            draggingInventoryItem = false;
            pressedComponent = null;
            return true;
        }
        UiComponent target = pressedComponent;
        pressedComponent = null;
        return target != null && target.mouseReleased(mouseX, mouseY, button)
                || super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (closingModal != null) return true;
        if (draggingInventoryItem && button == carriedMouseButton) return true;
        return pressedComponent != null
                && pressedComponent.mouseDragged(mouseX, mouseY, button, dragX, dragY)
                || super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        ensureThemeLayout();
        Select<?> select = openSelect();
        if (select != null) {
            select.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
            return true;
        }
        if (modal != null) {
            modal.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
            return true;
        }
        if (closingModal != null) return true;
        return root.mouseScrolled(mouseX, mouseY, scrollX, scrollY)
                || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        ensureThemeLayout();
        if (closingModal != null) return true;
        Select<?> select = openSelect();
        if (select != null && select.keyPressed(keyCode, scanCode, modifiers)) return true;
        if (modal != null && keyCode == GLFW.GLFW_KEY_ESCAPE) {
            closeModal();
            return true;
        }
        if (focusedComponent != null && !focusedComponent.isEnabled()) {
            clearToolkitFocus();
        }
        if (keyCode == GLFW.GLFW_KEY_TAB) {
            moveFocus((modifiers & GLFW.GLFW_MOD_SHIFT) != 0);
            return true;
        }
        if (focusedComponent != null && focusedComponent.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (modal != null) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (closingModal != null) return true;
        return focusedComponent != null && focusedComponent.charTyped(codePoint, modifiers)
                || super.charTyped(codePoint, modifiers);
    }

    private void moveFocus(boolean backwards) {
        List<UiComponent> focusable = new ArrayList<>();
        activeRoot().visitFocusable(focusable::add);
        if (focusable.isEmpty()) {
            clearToolkitFocus();
            return;
        }
        int current = focusable.indexOf(focusedComponent);
        int next = backwards
                ? (current <= 0 ? focusable.size() - 1 : current - 1)
                : (current + 1) % focusable.size();
        focus(focusable.get(next));
    }

    /** Current toolkit focus, for hosts that rebuild their component tree after an action. */
    protected final UiComponent focusedComponent() { return focusedComponent; }

    /** Moves focus to a replacement component after relayout; null clears it. */
    protected final void focusComponent(UiComponent component) {
        if (component == null) clearToolkitFocus();
        else focus(component);
    }

    private void focus(UiComponent component) {
        clearToolkitFocus();
        if (component.isFocusable() && component.isEnabled()) {
            focusedComponent = component;
            focusedComponent.setFocused(true);
        }
    }

    private void clearToolkitFocus() {
        if (focusedComponent != null) {
            focusedComponent.setFocused(false);
            focusedComponent = null;
        }
    }

    private UiComponent activeRoot() {
        return modal == null ? root : modal;
    }

    /** Only a focused, still-visible select can own an overlay or intercept input. */
    private Select<?> openSelect() {
        if (closingModal != null || !(focusedComponent instanceof Select<?> select) || !select.isOpen()) return null;
        boolean[] attached = {false};
        activeRoot().visitFocusable(component -> { if (component == select) attached[0] = true; });
        if (!attached[0] || select.bounds().width() == 0 || select.bounds().height() == 0) {
            clearToolkitFocus();
            return null;
        }
        select.layoutPopup(font, theme(), width, height);
        return select.isOpen() ? select : null;
    }

    public void showModal(Modal modal) {
        ensureThemeLayout();
        Objects.requireNonNull(modal, "modal");
        closeModal();
        this.modal = modal;
        closingModal = null;
        modal.beginOpen();
        modal.layoutInScreen(font, theme(), width, height);
        pressedComponent = null;
        clearToolkitFocus();
        List<UiComponent> focusable = new ArrayList<>();
        modal.visitFocusable(focusable::add);
        if (!focusable.isEmpty()) focus(focusable.get(0));
    }

    public void closeModal() {
        Modal closing = modal;
        if (closing == null) return;
        modal = null;
        closing.beginClose();
        closingModal = closing;
        pressedComponent = null;
        clearToolkitFocus();
        focusedComponent = null;
        closing.closed();
    }

    @Override
    public void removed() {
        clearToolkitFocus();
        clearCarriedItem();
        super.removed();
    }

    private boolean handleInventoryPickerClick(UiComponent target, double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT && button != GLFW.GLFW_MOUSE_BUTTON_RIGHT) return false;

        if (target instanceof ItemSlot itemSlot && itemSlot.isEnabled() && !carriedItem.isEmpty()) {
            clearToolkitFocus();
            focus(itemSlot);
            itemSlot.acceptDrop(carriedItem);
            clearCarriedItem();
            pressedComponent = null;
            return true;
        }

        if (!(target instanceof InventoryGrid grid)
                || grid.interactionMode() != InventoryGrid.InteractionMode.PICKER) return false;
        int slotIndex = grid.slotIndexAt(mouseX, mouseY);
        if (slotIndex < 0) return false;

        ItemStack selected = grid.itemAt(slotIndex);
        if (selected.isEmpty()) return false;
        if (grid == carriedFromGrid && slotIndex == carriedFromSlot && !carriedItem.isEmpty()) {
            clearCarriedItem();
            return true;
        }

        clearToolkitFocus();
        if (carriedFromGrid != null) carriedFromGrid.setCarriedSlotIndex(-1);
        carriedItem = selected.copy();
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) carriedItem.setCount(1);
        carriedFromGrid = grid;
        carriedFromSlot = slotIndex;
        carriedMouseButton = button;
        draggingInventoryItem = true;
        grid.setCarriedSlotIndex(slotIndex);
        grid.notifyPicked(slotIndex, carriedItem);
        pressedComponent = null;
        return true;
    }

    private void clearCarriedItem() {
        if (carriedFromGrid != null) carriedFromGrid.setCarriedSlotIndex(-1);
        carriedItem = ItemStack.EMPTY;
        carriedFromGrid = null;
        carriedFromSlot = -1;
        carriedMouseButton = -1;
        draggingInventoryItem = false;
    }

    /** Renders the current picker stack above components and tooltips. */
    protected final void renderCarriedItem(GuiGraphics graphics, int mouseX, int mouseY) {
        if (carriedItem.isEmpty()) return;
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, CARRIED_ITEM_Z_INDEX);
        graphics.renderItem(carriedItem, mouseX - 8, mouseY - 8);
        graphics.renderItemDecorations(font, carriedItem, mouseX - 8, mouseY - 8);
        graphics.pose().popPose();
    }

    public Modal modal() {
        return modal;
    }

    public UiComponent root() {
        return root;
    }

    /** Panel area reserved by JEI. Override when the root fills the viewport around a panel. */
    public Bounds jeiGuiBounds() {
        return root.bounds();
    }

    public UiTheme theme() {
        return themeOverride == null ? UiThemes.globalTheme() : themeOverride;
    }

    public ToastHost toastHost() {
        return toastHost;
    }

    protected final void ensureThemeLayout() {
        UiTheme activeTheme = theme();
        if (activeTheme == layoutTheme || font == null) return;
        layoutRoot(activeTheme);
        toastHost.layout(font, activeTheme, new Bounds(0, 0, width, height));
        if (modal != null) modal.layoutInScreen(font, activeTheme, width, height);
        if (closingModal != null) closingModal.layoutInScreen(font, activeTheme, width, height);
        layoutTheme = activeTheme;
    }
}
