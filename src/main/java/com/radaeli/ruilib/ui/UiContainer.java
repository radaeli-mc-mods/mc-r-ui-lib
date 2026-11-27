package com.radaeli.ruilib.ui;

import net.minecraft.client.gui.Font;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/** Base container with stable render order and reverse (topmost-first) hit testing. */
public abstract class UiContainer extends UiComponent {
    private final List<UiComponent> children = new ArrayList<>();

    public <T extends UiComponent> T add(T child) {
        Objects.requireNonNull(child, "child");
        if (child == this || containsDescendant(child, this)) {
            throw new IllegalArgumentException("Adding this component would create a UI cycle");
        }
        if (children.contains(child)) {
            throw new IllegalArgumentException("A component can only appear once in a container");
        }
        children.add(child);
        return child;
    }

    private static boolean containsDescendant(UiComponent root, UiComponent target) {
        if (root == target) {
            return true;
        }
        if (root instanceof UiContainer container) {
            for (UiComponent child : container.children) {
                if (containsDescendant(child, target)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean remove(UiComponent child) {
        return children.remove(child);
    }

    public List<UiComponent> children() {
        return Collections.unmodifiableList(children);
    }

    @Override
    public void visitComponents(Consumer<UiComponent> visitor) {
        visitor.accept(this);
        for (UiComponent child : children) child.visitComponents(visitor);
    }

    @Override
    public void render(UiContext context) {
        for (UiComponent child : children) {
            child.render(context);
        }
    }

    @Override public UiComponent inspectionHitTest(double x,double y){
        if(!bounds().contains(x,y))return null;
        for(int i=children.size()-1;i>=0;i--){var hit=children.get(i).inspectionHitTest(x,y);if(hit!=null)return hit;}
        return this;
    }
    @Override
    public UiComponent hitTest(double mouseX, double mouseY) {
        if (!isEnabled() || !bounds().contains(mouseX, mouseY)) {
            return null;
        }
        for (int index = children.size() - 1; index >= 0; index--) {
            UiComponent hit = children.get(index).hitTest(mouseX, mouseY);
            if (hit != null) {
                return hit;
            }
        }
        return null;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!isEnabled() || !bounds().contains(mouseX, mouseY)) return false;
        for (int index = children.size() - 1; index >= 0; index--) {
            if (children.get(index).mouseScrolled(mouseX, mouseY, scrollX, scrollY)) return true;
        }
        return false;
    }

    @Override
    public void visitFocusable(Consumer<UiComponent> visitor) {
        if (!isEnabled()) {
            return;
        }
        for (UiComponent child : children) {
            child.visitFocusable(visitor);
        }
    }

    protected UiSize measureChildren(Font font, UiTheme theme, int maxWidth, int maxHeight,
                                     int gap, boolean horizontal) {
        int primary = 0;
        int cross = 0;
        for (UiComponent child : children) {
            UiSize size = child.measure(font, theme, maxWidth, maxHeight);
            primary += horizontal ? size.width() : size.height();
            cross = Math.max(cross, horizontal ? size.height() : size.width());
        }
        if (children.size() > 1) {
            primary += gap * (children.size() - 1);
        }
        return (horizontal ? new UiSize(primary, cross) : new UiSize(cross, primary))
                .constrain(maxWidth, maxHeight);
    }

    protected List<UiComponent> mutableChildren() {
        return children;
    }
}
