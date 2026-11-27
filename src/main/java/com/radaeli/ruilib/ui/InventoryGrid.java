package com.radaeli.ruilib.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Displays a live Container as themed item slots. The player factory lays out the main inventory
 * and hotbar separately; generic containers are rendered as a regular row-major grid.
 *
 * <p>Picker interactions are routed by {@link UiScreen}, which keeps the picked stack as a
 * temporary reference and can deliver it to an {@link ItemSlot} without changing this container.
 */
public final class InventoryGrid extends UiComponent {
    private static final int DEFAULT_COLUMNS = 9;
    private static final int DEFAULT_SLOT_SIZE = 18;
    private static final int DEFAULT_GAP = 2;
    private static final int HOTBAR_GAP = 5;

    public enum InteractionMode {
        /** Click and drag produce a temporary stack reference for dropping into an ItemSlot. */
        PICKER,
        /** Pointer interactions are reported to the consumer; the component never changes a Container. */
        CUSTOM
    }

    public enum Action {
        PRESS,
        DRAG,
        RELEASE
    }

    /** Immutable interaction snapshot. The ItemStack accessor always returns a defensive copy. */
    public record InteractionEvent(Action action, int slotIndex, int mouseButton, ItemStack stack) {
        public InteractionEvent {
            Objects.requireNonNull(action, "action");
            stack = Objects.requireNonNull(stack, "stack").copy();
        }

        @Override
        public ItemStack stack() {
            return stack.copy();
        }
    }

    private final Supplier<? extends Container> containerSupplier;
    private final boolean playerLayout;
    private int columns = DEFAULT_COLUMNS;
    private int slotSize = DEFAULT_SLOT_SIZE;
    private boolean explicitSlotSize;
    private int gap = DEFAULT_GAP;
    private int carriedSlotIndex = -1;
    private int customPressedSlot = -1;
    private int customPressedButton = -1;
    private InteractionMode interactionMode = InteractionMode.PICKER;
    private Consumer<ItemStack> onItemPicked = ignored -> { };
    private BiConsumer<Integer, ItemStack> onItemPickedWithSlot = (slot, stack) -> { };
    private Consumer<InteractionEvent> onInteraction = ignored -> { };

    /** Creates a row-major grid backed by a consumer-provided Container. */
    public InventoryGrid(Container container) {
        this(fixedContainer(container), false);
    }

    /** Creates a row-major grid with explicit columns, slot size, and gap. */
    public InventoryGrid(Container container, int columns, int slotSize, int gap) {
        this(fixedContainer(container), false);
        setColumns(columns);
        setSlotSize(slotSize);
        setGap(gap);
    }

    private InventoryGrid(Supplier<? extends Container> containerSupplier, boolean playerLayout) {
        this.containerSupplier = Objects.requireNonNull(containerSupplier, "containerSupplier");
        this.playerLayout = playerLayout;
    }

    private static Supplier<Container> fixedContainer(Container container) {
        Objects.requireNonNull(container, "container");
        return () -> container;
    }

    /** Creates a grid that follows the current local player's 27 main slots and 9 hotbar slots. */
    public static InventoryGrid playerInventory() {
        return new InventoryGrid(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            return minecraft.player == null ? null : minecraft.player.getInventory();
        }, true);
    }

    public InventoryGrid setColumns(int columns) {
        if (columns < 1 || columns > 64) throw new IllegalArgumentException("columns must be 1 to 64");
        this.columns = columns;
        return this;
    }

    public int columns() {
        return columns;
    }

    public InventoryGrid setSlotSize(int slotSize) {
        if (slotSize < 16 || slotSize > 128) throw new IllegalArgumentException("slotSize must be 16 to 128");
        this.slotSize = slotSize;
        explicitSlotSize=true;
        return this;
    }

    public int slotSize() {
        return slotSize;
    }

    public InventoryGrid setGap(int gap) {
        if (gap < 0 || gap > 32) throw new IllegalArgumentException("gap must be 0 to 32");
        this.gap = gap;
        return this;
    }

    public int gap() {
        return gap;
    }

    public InventoryGrid setInteractionMode(InteractionMode interactionMode) {
        this.interactionMode = Objects.requireNonNull(interactionMode, "interactionMode");
        customPressedSlot = -1;
        return this;
    }

    public InteractionMode interactionMode() {
        return interactionMode;
    }

    /** Called with a defensive copy when Picker mode begins a selection. */
    public InventoryGrid setOnItemPicked(Consumer<ItemStack> onItemPicked) {
        this.onItemPicked = Objects.requireNonNull(onItemPicked, "onItemPicked");
        return this;
    }

    /** Receives the displayed inventory slot index together with the temporary picked stack. */
    public InventoryGrid setOnItemPickedWithSlot(BiConsumer<Integer, ItemStack> onItemPicked) {
        this.onItemPickedWithSlot = Objects.requireNonNull(onItemPicked, "onItemPicked");
        return this;
    }

    /** Called for press, drag, and release snapshots in Custom mode. */
    public InventoryGrid setOnInteraction(Consumer<InteractionEvent> onInteraction) {
        this.onInteraction = Objects.requireNonNull(onInteraction, "onInteraction");
        return this;
    }

    /** Returns a copy of the current stack at a container index, or EMPTY for an invalid index. */
    public ItemStack itemAt(int slotIndex) {
        Container container = containerSupplier.get();
        if (container == null || slotIndex < 0 || slotIndex >= visibleSlotCount(container)) {
            return ItemStack.EMPTY;
        }
        return container.getItem(containerSlotIndex(slotIndex)).copy();
    }

    @Override
    protected UiSize measureIntrinsic(Font font, UiTheme theme, int maxWidth, int maxHeight) {
        if(!explicitSlotSize)slotSize=theme.skin().pixelated()?20:DEFAULT_SLOT_SIZE;
        int rows = rowCount();
        int width = slotSize * columns + gap * Math.max(0, columns - 1);
        int height = slotSize * rows + gap * Math.max(0, rows - 1)
                + (playerLayout ? HOTBAR_GAP : 0);
        return new UiSize(width, height).constrain(maxWidth, maxHeight);
    }

    @Override protected void layoutChildren(Font f,UiTheme t){if(!explicitSlotSize)slotSize=t.skin().pixelated()?20:DEFAULT_SLOT_SIZE;}
    @Override
    public void render(UiContext context) {
        Container container = containerSupplier.get();
        if (container == null) return;

        int count = visibleSlotCount(container);
        if (playerLayout && count > 27) {
            int hotbarRows = (9 + columns - 1) / columns;
            int firstHotbarRow = playerMainRows();
            int bandHeight = hotbarRows * slotSize + Math.max(0, hotbarRows - 1) * gap;
            Bounds hotbarBand = new Bounds(bounds().x(), rowY(firstHotbarRow), bounds().width(), bandHeight);
            if(!context.theme().skin().pixelated())UiPaint.roundedRect(context, hotbarBand,
                    UiPaint.withAlpha(context.theme().palette().accent(), 0.07),
                    context.theme().tokens().radii().small());
            UiPaint.divider(context, new Bounds(bounds().x(), rowY(firstHotbarRow) - (HOTBAR_GAP - 1) / 2,
                    bounds().width(), 1));
        }

        for (int displayIndex = 0; displayIndex < count; displayIndex++) {
            int x = columnX(displayIndex);
            int y = rowY(rowForDisplayIndex(displayIndex));
            Bounds slotBounds = new Bounds(x, y, slotSize, slotSize);
            boolean hovered = isEnabled() && slotBounds.contains(context.mouseX(), context.mouseY());
            double hover = animate(context, "slot-hover-" + displayIndex, hovered ? 1 : 0);
            double selected = animate(context, "slot-selected-" + displayIndex,
                    carriedSlotIndex == displayIndex ? 1 : 0);
            if(context.theme().skin().pixelated()){
                context.theme().skin().surface(context,slotBounds,SurfaceVariant.INSET,WorkshopSkin.WELL);
                if(selected>0||hover>0)UiPaint.outline(context,slotBounds.inset(Insets.all(1)),selected>0?context.theme().palette().accent():WorkshopSkin.EDGE_LIGHT);
            }else UiPaint.card(context, slotBounds, hover, selected, 0);
            if (playerLayout && displayIndex >= 27 && !context.theme().skin().pixelated()) {
                UiPaint.roundedRect(context, slotBounds,
                        UiPaint.withAlpha(context.theme().palette().accent(), 0.10),
                        context.theme().tokens().radii().medium());
                UiPaint.roundedOutline(context, slotBounds, context.theme().palette().border(),
                        context.theme().tokens().radii().medium());
            }

            ItemStack stack = container.getItem(containerSlotIndex(displayIndex));
            if (!stack.isEmpty()) {
                int itemOffset = (slotSize - 16) / 2;
                ItemRenderer.render(context, stack, x + itemOffset, y + itemOffset);
                if (hovered) ItemRenderer.renderTooltip(context, stack);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isEnabled() || interactionMode != InteractionMode.CUSTOM) return false;
        int slot = slotIndexAt(mouseX, mouseY);
        if (slot < 0) return false;
        customPressedSlot = slot;
        customPressedButton = button;
        onInteraction.accept(new InteractionEvent(Action.PRESS, slot, button, itemAt(slot)));
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!isEnabled() || interactionMode != InteractionMode.CUSTOM || customPressedSlot < 0) return false;
        int slot = slotIndexAt(mouseX, mouseY);
        onInteraction.accept(new InteractionEvent(Action.DRAG, slot, customPressedButton,
                slot < 0 ? ItemStack.EMPTY : itemAt(slot)));
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (interactionMode != InteractionMode.CUSTOM || customPressedSlot < 0) return false;
        int slot = slotIndexAt(mouseX, mouseY);
        onInteraction.accept(new InteractionEvent(Action.RELEASE, slot, customPressedButton,
                slot < 0 ? ItemStack.EMPTY : itemAt(slot)));
        customPressedSlot = -1;
        customPressedButton = -1;
        return true;
    }

    int slotIndexAt(double mouseX, double mouseY) {
        if (!isEnabled() || !bounds().contains(mouseX, mouseY)) return -1;
        int localX = (int) mouseX - bounds().x();
        int columnPitch = slotSize + gap;
        int column = localX / columnPitch;
        if (column < 0 || column >= columns || localX % columnPitch >= slotSize) return -1;

        int row = -1;
        int localY = (int) mouseY - bounds().y();
        for (int candidate = 0; candidate < rowCount(); candidate++) {
            int top = rowY(candidate) - bounds().y();
            if (localY >= top && localY < top + slotSize) {
                row = candidate;
                break;
            }
        }
        if (row < 0) return -1;
        int displayIndex = displayIndexAt(row, column);
        Container container = containerSupplier.get();
        return container != null && displayIndex < visibleSlotCount(container) ? displayIndex : -1;
    }

    void setCarriedSlotIndex(int carriedSlotIndex) {
        this.carriedSlotIndex = carriedSlotIndex;
    }

    void notifyPicked(int slotIndex, ItemStack stack) {
        onItemPicked.accept(stack.copy());
        onItemPickedWithSlot.accept(slotIndex, stack.copy());
    }

    private int rowCount() {
        if (playerLayout) return playerMainRows() + (9 + columns - 1) / columns;
        Container container = containerSupplier.get();
        if (container == null || container.getContainerSize() <= 0) return 0;
        return (container.getContainerSize() + columns - 1) / columns;
    }

    private int visibleSlotCount(Container container) {
        return playerLayout ? Math.min(36, container.getContainerSize()) : container.getContainerSize();
    }

    private int containerSlotIndex(int displayIndex) {
        if (!playerLayout) return displayIndex;
        return displayIndex < 27 ? displayIndex + 9 : displayIndex - 27;
    }

    private int columnX(int displayIndex) {
        return bounds().x() + columnForDisplayIndex(displayIndex) * (slotSize + gap);
    }

    private int rowY(int row) {
        int y = bounds().y() + row * (slotSize + gap);
        if (playerLayout && row >= playerMainRows()) y += HOTBAR_GAP;
        return y;
    }

    private int playerMainRows() {
        return (27 + columns - 1) / columns;
    }

    private int rowForDisplayIndex(int displayIndex) {
        if (!playerLayout || displayIndex < 27) return displayIndex / columns;
        return playerMainRows() + (displayIndex - 27) / columns;
    }

    private int columnForDisplayIndex(int displayIndex) {
        return playerLayout && displayIndex >= 27
                ? (displayIndex - 27) % columns : displayIndex % columns;
    }

    private int displayIndexAt(int row, int column) {
        if (!playerLayout) return row * columns + column;
        if (row < playerMainRows()) {
            int mainIndex = row * columns + column;
            return mainIndex < 27 ? mainIndex : -1;
        }
        int hotbarIndex = 27 + (row - playerMainRows()) * columns + column;
        return hotbarIndex < 36 ? hotbarIndex : -1;
    }
}
