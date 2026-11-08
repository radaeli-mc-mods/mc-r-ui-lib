package com.radaeli.ruilib.client.playground;

import com.radaeli.ruilib.ui.Alignment;
import com.radaeli.ruilib.ui.Slider;
import net.minecraft.resources.ResourceLocation;

final class PlaygroundDocument {
    static final int CURRENT_VERSION = 1;

    int version = CURRENT_VERSION;
    String name = "untitled";
    PlaygroundNode root = PlaygroundNode.create("absolute", 0, 0);

    static PlaygroundDocument empty(String name) {
        PlaygroundDocument document = new PlaygroundDocument();
        document.name = name;
        document.root = PlaygroundNode.create("absolute", 0, 0);
        document.root.width = 0;
        document.root.height = 0;
        return document;
    }

    void normalize() {
        if (version != CURRENT_VERSION) {
            throw new IllegalArgumentException("Unsupported playground document version: " + version);
        }
        if (name == null || name.isBlank()) name = "untitled";
        if (root == null || !root.isAbsolute()) {
            throw new IllegalArgumentException("Document root must be an absolute layout");
        }
        normalizeNode(root, null, 0, new int[]{0});
    }

    private static void normalizeNode(PlaygroundNode node, PlaygroundNode parent, int depth, int[] count) {
        if (depth > 32) throw new IllegalArgumentException("Layout is nested too deeply");
        if (node == null || node.type == null) throw new IllegalArgumentException("Layout contains an empty node");
        if (++count[0] > 512) throw new IllegalArgumentException("Layout has more than 512 components");
        if (node.children == null) node.children = new java.util.ArrayList<>();
        if (!(node.type.equals("segmented") || node.type.equals("itempreview") || node.type.equals("colorswatch") || node.type.equals("colorpicker") || node.type.equals("label") || node.type.equals("button")
                || node.type.equals("iconbutton") || node.type.equals("divider")
                || node.type.equals("toggle") || node.type.equals("chip")
                || node.type.equals("radio") || node.type.equals("radiogroup") || node.type.equals("stepper")
                || node.type.equals("textfield") || node.type.equals("searchfield")
                || node.type.equals("slider") || node.type.equals("progressbar")
                || node.type.equals("radial") || node.type.equals("itemcard") || node.type.equals("itemslot")
                || node.type.equals("table") || node.type.equals("curvechart")
                || node.type.equals("spacer") || node.isContainer())) {
            throw new IllegalArgumentException("Unsupported component type: " + node.type);
        }
        if (!node.isContainer() && !node.children.isEmpty()) {
            throw new IllegalArgumentException("Only layout containers can have children");
        }
        if (node.iconSymbol == null) node.iconSymbol="";
        if (!node.iconSymbol.isBlank()) com.radaeli.ruilib.ui.IconButton.Symbol.valueOf(node.iconSymbol);
        if (node.text == null) node.text = node.type;
        if (node.horizontalAlignment == null) node.horizontalAlignment = Alignment.START;
        if (node.verticalAlignment == null) node.verticalAlignment = Alignment.STRETCH;
        node.selectedTab = Math.max(0, Math.min(node.selectedTab, Math.max(0, node.children.size() - 1)));
        if (node.width < 0 || node.width > 8192 || node.height < 0 || node.height > 8192
                || node.gap < 0 || node.gap > 4096 || Math.abs((long) node.x) > 100_000
                || Math.abs((long) node.y) > 100_000) {
            throw new IllegalArgumentException("Component dimensions or position are outside supported limits");
        }
        if (node.supportsColumns() && (node.columns < 1 || node.columns > 64)) {
            throw new IllegalArgumentException("Grid columns must be between 1 and 64");
        }
        if (node.usesItemStack()) {
            if (node.type.equals("itemcard")
                    && (node.itemId == null || ResourceLocation.tryParse(node.itemId) == null)) {
                throw new IllegalArgumentException("Item card requires a valid namespaced item ID");
            }
            if (node.type.equals("itemslot") && node.itemId != null && !node.itemId.isBlank()
                    && ResourceLocation.tryParse(node.itemId) == null) {
                throw new IllegalArgumentException("Item slot item ID must be empty or a valid namespaced ID");
            }
            if (node.supportsItemCount()) node.itemCount = Math.max(1, Math.min(64, node.itemCount));
            if (node.text == null || node.text.isBlank()) node.text = node.itemId;
        }
        if (node.type.equals("slider")) {
            Slider slider = new Slider(node.minimum, node.maximum, node.value, node.step, ignored -> { });
            if (node.upperValue != null) slider.setValues(node.value, node.upperValue);
            node.value = slider.value();
            node.upperValue = slider.upperValue();
        }
        if (node.type.equals("progressbar")) {
            if (!Double.isFinite(node.minimum) || !Double.isFinite(node.maximum) || !Double.isFinite(node.value)) {
                throw new IllegalArgumentException("ProgressBar values must be finite");
            }
            if (node.maximum > node.minimum) {
                node.value = Math.max(node.minimum, Math.min(node.maximum, node.value));
            }
        }
        if (node.type.equals("curvechart")) {
            node.text = PlaygroundComponentFactory.formatCurvePoints(
                    PlaygroundComponentFactory.parseCurvePoints(node.text));
        }
        node.parent = parent;
        for (PlaygroundNode child : node.children) normalizeNode(child, node, depth + 1, count);
    }
}
