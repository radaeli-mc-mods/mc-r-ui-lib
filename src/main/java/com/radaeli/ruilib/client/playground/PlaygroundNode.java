package com.radaeli.ruilib.client.playground;

import com.radaeli.ruilib.ui.Alignment;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/** JSON-friendly description of one node in a playground document. */
final class PlaygroundNode {
    String type = "label";
    String text = "Label";
    String iconSymbol = "";
    Integer background;
    int color = 0xFFF0F3F7;
    boolean enabled = true;
    boolean selected;
    boolean flexible;
    boolean percentage;
    int selectedTab;
    int columns = 3;
    int itemCount = 1;
    String itemId = "minecraft:diamond";
    String itemSlotBackground = "theme";
    double minimum;
    double maximum = 100;
    double value = 50;
    Double upperValue;
    double step = 1;
    int x;
    int y;
    int width = 100;
    int height = 20;
    int gap = 8;
    Alignment horizontalAlignment = Alignment.START;
    Alignment verticalAlignment = Alignment.STRETCH;
    List<PlaygroundNode> children = new ArrayList<>();

    transient PlaygroundNode parent;

    static PlaygroundNode create(String type, int x, int y) {
        PlaygroundNode node = new PlaygroundNode();
        node.type = type;
        node.x = x;
        node.y = y;
        switch (type) {
            case "label" -> {
                node.text = "Text";
                node.width = 80;
                node.height = 12;
            }
            case "button" -> {
                node.text = "Button";
                node.width = 100;
                node.height = 22;
            }
            case "iconbutton" -> {
                node.text = "Settings";
                node.width = 24;
                node.height = 22;
            }
            case "divider" -> {
                node.text = "Horizontal";
                node.width = 120;
                node.height = 1;
            }
            case "panel", "card", "selectablecard" -> {
                node.text = type.equals("card") ? "Card" : "Panel";
                node.width = 160;
                node.height = 90;
            }
            case "segmented" -> { node.text="Mode"; node.width=240; node.height=26; }
            case "radio" -> { node.text = "Radio"; node.width = 112; node.height = 16; }
            case "radiogroup" -> { node.text = "Mode"; node.width = 160; node.height = 54; }
            case "stepper" -> { node.text = "Range"; node.width = 160; node.height = 12; node.maximum = 32;
                node.value = 4; }
            case "toggle" -> {
                node.text = "Toggle";
                node.width = 112;
                node.height = 22;
            }
            case "chip" -> {
                node.text = "Chip";
                node.width = 64;
                node.height = 20;
            }
            case "textfield" -> {
                node.text = "";
                node.width = 140;
                node.height = 22;
            }
            case "searchfield" -> {
                node.text = "";
                node.width = 140;
                node.height = 22;
            }
            case "slider" -> {
                node.text = "Slider";
                node.width = 160;
                node.height = 22;
            }
            case "progressbar" -> {
                node.text = "Progress";
                node.width = 180;
                node.height = 18;
            }
            case "radial" -> {
                node.text = "North, East, South, West";
                node.width = 144;
                node.height = 144;
            }
            case "curvechart" -> {
                node.text = "-38:1.5:-0.006; 63:1:-0.004; 280:0.4:-0.0016; 320:0:-0.02";
                node.width = 320;
                node.height = 180;
            }
            case "itempreview" -> { node.width=64; node.height=64; }
            case "colorswatch" -> { node.width=28; node.height=28; }
            case "colorpicker" -> { node.width=220; node.height=210; }
            case "hscroll" -> { node.width=240; node.height=80; }
            case "itemcard" -> {
                node.text = "Diamond";
                node.itemId = "minecraft:diamond";
                node.width = 116;
                node.height = 32;
            }
            case "itemslot" -> {
                node.text = "";
                node.itemId = "";
                node.itemCount = 1;
                node.width = 24;
                node.height = 24;
            }
            case "itemgrid", "grid" -> {
                node.text = type.equals("grid") ? "Grid" : "ItemGrid";
                node.width = 180;
                node.height = 120;
                node.columns = 3;
                node.gap = 4;
            }
            case "table" -> {
                node.text = "Name, Amount, Status";
                node.width = 220;
                node.height = 112;
                node.columns = 3;
            }
            case "spacer" -> {
                node.text = "Spacer";
                node.width = 16;
                node.height = 16;
            }
            case "tabs" -> {
                node.text = "Tabs";
                node.width = 180;
                node.height = 110;
            }
            case "scroll", "clip" -> {
                node.text = type.equals("scroll") ? "ScrollView" : "ClipStack";
                node.width = 150;
                node.height = 90;
            }
            case "row", "column", "stack", "absolute" -> {
                node.text = type.substring(0, 1).toUpperCase() + type.substring(1);
                node.width = 150;
                node.height = 90;
            }
            default -> throw new IllegalArgumentException("Unsupported component type: " + type);
        }
        return node;
    }

    boolean isContainer() {
        return type.equals("selectablecard") || type.equals("panel") || type.equals("card")
                || type.equals("row") || type.equals("column")
                || type.equals("stack") || type.equals("absolute") || type.equals("tabs")
                || type.equals("hscroll") || type.equals("scroll") || type.equals("clip") || type.equals("grid")
                || type.equals("itemgrid");
    }

    boolean usesItemStack() {
        return type.equals("itempreview") || type.equals("itemcard") || type.equals("itemslot");
    }

    boolean supportsItemCount() {
        return type.equals("itemcard");
    }

    boolean supportsColumns() {
        return type.equals("grid") || type.equals("itemgrid") || type.equals("table");
    }

    boolean supportsAlignment() {
        return type.equals("hscroll") || type.equals("row") || type.equals("column") || type.equals("stack");
    }

    boolean isAbsolute() {
        return type.equals("absolute");
    }

    void addChild(PlaygroundNode child, int index) {
        child.parent = this;
        children.add(Math.max(0, Math.min(index, children.size())), child);
    }

    boolean removeChild(PlaygroundNode child) {
        if (children.remove(child)) {
            child.parent = null;
            return true;
        }
        return false;
    }

    boolean contains(PlaygroundNode candidate) {
        if (this == candidate) return true;
        for (PlaygroundNode child : children) {
            if (child.contains(candidate)) return true;
        }
        return false;
    }
}
