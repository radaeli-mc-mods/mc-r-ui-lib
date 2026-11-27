package com.radaeli.ruilib.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.List;

/** Deterministic development fixture for popup, modal, toast and inventory overlays. */
public final class UiOverlayValidation {
    private static Modal modal;
    private static ToastHost toasts;
    private static Select<Integer> select;
    private static InventoryGrid inventory;
    private static ItemSlot slot;
    private static TextField cursorField;

    private UiOverlayValidation() { }
    public static void renderCursor(GuiGraphics graphics, Font font, int width, int height, int step) {
        if (step == 0) {
            cursorField = new TextField();
            cursorField.setValue("Blink test");
            cursorField.layout(font, UiTheme.DARK, new Bounds(40, 40, 180, 24));
            cursorField.setFocused(true);
        }
        cursorField.render(new UiContext(graphics, font, UiTheme.DARK, -100, -100, width, height, step * 300_000_000L));
    }
    public static void render(GuiGraphics graphics, Font font, int width, int height, int step) {render(graphics,font,width,height,step,UiTheme.DARK);}
    public static void render(GuiGraphics graphics, Font font, int width, int height, int step,UiTheme theme) {
        if (step == 0) {
            modal = new Modal(Component.literal("Modal transition"), new Label(Component.literal("Shared content")), 240, 100);
            modal.beginOpen();
            modal.layoutInScreen(font, theme, width, height);
            toasts = new ToastHost();
            toasts.layout(font, theme, new Bounds(0, 0, width, height));
            toasts.success(Component.literal("Shared toast"));
            select = new Select<>(List.of(new Select.Option<>(0, Component.literal("First")),
                    new Select.Option<>(1, Component.literal("Second"))), 0, value -> { });
            select.layout(font, theme, new Bounds(20, 20, 150, 24));
            select.mouseClicked(30, 30, 0); select.mouseReleased(30, 30, 0);
            if(theme.skin().pixelated()){
                Column body=new Column(6);body.add(new Label(Component.literal("Select inside dialog")));body.add(select);
                select.setPreferredSize(180,20);
                modal=new Modal(Component.literal("Workshop dialog"),body,240,120);modal.beginOpen();modal.layoutInScreen(font,theme,width,height);
                select.setFocused(true);select.keyPressed(257,0,0);
            }
            select.layoutPopup(font, theme, width, height);
            SimpleContainer container = new SimpleContainer(9);
            container.setItem(0, new ItemStack(Items.DIAMOND, 32));
            container.setItem(1, new ItemStack(Items.IRON_INGOT, 12));
            inventory = new InventoryGrid(container);
            inventory.layout(font, theme, new Bounds(20, height - 50, 200, 24));
            slot = new ItemSlot(new ItemStack(Items.DIAMOND));
            slot.layout(font, theme, new Bounds(240, height - 50, 24, 24));
        }
        UiContext context = new UiContext(graphics, font, theme, -100, -100, width, height, step * 35_000_000L);
        inventory.render(context);
        slot.render(context);
        modal.render(context);
        graphics.pose().pushPose();
        graphics.pose().translate(0,0,400);
        select.render(context);
        select.renderPopup(context);
        graphics.pose().popPose();
        graphics.pose().pushPose();
        graphics.pose().translate(0,0,420);
        toasts.render(context);
        graphics.pose().popPose();
    }
}
