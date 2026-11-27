package com.radaeli.ruilib.platform;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.gui.components.EditBox;

public final class UiPlatform {
    private UiPlatform() { }
    public static void icon(GuiGraphics g, ResourceLocation texture,int x,int y,int u,int v,int w,int h,int tw,int th,int color){
        g.setColor(((color>>16)&255)/255f,((color>>8)&255)/255f,(color&255)/255f,(color>>>24)/255f);
        try { g.blit(texture,x,y,u,v,w,h,tw,th); } finally { g.setColor(1,1,1,1); }
    }
    public static ResourceLocation id(String namespace, String path) { return ResourceLocation.tryParse(namespace + ":" + path); }
    public static void tickEditor(EditBox editor) { editor.tick(); }
    public static boolean scroll(UiPlatformScreen screen, double x, double y, double delta) {
        return screen.mouseScrolled(x, y, delta);
    }
    public static void outline(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.renderOutline(x, y, width, height, color);
    }
}
