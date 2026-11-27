package com.radaeli.ruilib.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Mouse gesture radial menu. The consumer controls when the component is open. */
public final class RadialMenu extends UiComponent {
    private static final double DEAD_ZONE_FRACTION = 0.28;

    private final List<RadialSegment> segments = new ArrayList<>();
    private boolean open = true;
    private boolean gestureActive;
    private int hoveredSegment = -1;

    public RadialMenu addSegment(RadialSegment segment) {
        segments.add(Objects.requireNonNull(segment, "segment"));
        return this;
    }

    public RadialMenu setSegments(List<RadialSegment> segments) {
        this.segments.clear();
        this.segments.addAll(List.copyOf(Objects.requireNonNull(segments, "segments")));
        hoveredSegment = -1;
        return this;
    }

    public List<RadialSegment> segments() {
        return List.copyOf(segments);
    }

    public RadialMenu open() {
        open = true;
        return this;
    }

    public RadialMenu close() {
        open = false;
        cancelGesture();
        return this;
    }

    public boolean isOpen() {
        return open;
    }

    public boolean isGestureActive() {
        return gestureActive;
    }

    public int hoveredSegment() {
        return hoveredSegment;
    }

    @Override
    protected UiSize measureIntrinsic(Font font, UiTheme theme, int maxWidth, int maxHeight) {
        int size = Math.min(144, Math.min(maxWidth, maxHeight));
        return new UiSize(Math.max(0, size), Math.max(0, size));
    }

    @Override
    public void render(UiContext context) {
        if (!isEnabled() || segments.isEmpty()) return;
        double openProgress = animateFrom(context, "open", open ? 1 : 0, open ? 0 : 1);
        if (openProgress <= 0) return;
        Bounds bounds = bounds();
        RadialGeometry.Point center = RadialGeometry.center(bounds);
        double outerRadius = RadialGeometry.outerRadius(bounds);
        if (outerRadius < 2) return;
        double innerRadius = RadialGeometry.innerRadius(outerRadius, DEAD_ZONE_FRACTION);
        double[] hoverProgress = new double[segments.size()];
        for (int index = 0; index < segments.size(); index++) {
            hoverProgress[index] = animate(context, "segment-hover-" + index,
                    index == hoveredSegment ? 1 : 0);
        }
        rasterizeSegments(context, center, innerRadius, outerRadius, hoverProgress, openProgress);
        drawCenter(context, center, innerRadius, openProgress);
        drawSegmentContents(context, center, innerRadius, outerRadius, openProgress);
    }

    private void rasterizeSegments(UiContext context, RadialGeometry.Point center,
                                   double innerRadius, double outerRadius, double[] hoverProgress,
                                   double openProgress) {
        int top = Math.max(bounds().y(), (int) Math.ceil(center.y() - outerRadius));
        int bottom = Math.min(bounds().bottom() - 1, (int) Math.floor(center.y() + outerRadius));
        int outerSquared = (int) Math.ceil(outerRadius * outerRadius);
        int innerSquared = (int) Math.floor(innerRadius * innerRadius);
        for (int y = top; y <= bottom; y++) {
            double dy = y + 0.5 - center.y();
            double outerSpan = Math.sqrt(Math.max(0, outerSquared - dy * dy));
            int left = Math.max(bounds().x(), (int) Math.ceil(center.x() - outerSpan));
            int right = Math.min(bounds().right() - 1, (int) Math.floor(center.x() + outerSpan));
            int runStart = -1;
            int runSegment = -1;
            for (int x = left; x <= right; x++) {
                double dx = x + 0.5 - center.x();
                double distanceSquared = dx * dx + dy * dy;
                int segment = distanceSquared <= innerSquared ? -1
                        : RadialGeometry.segmentAt(x + 0.5, y + 0.5, center,
                                innerRadius, outerRadius, segments.size());
                if (segment != runSegment) {
                    drawRun(context, y, runStart, x, runSegment, hoverProgress, openProgress);
                    runStart = segment < 0 ? -1 : x;
                    runSegment = segment;
                }
            }
            drawRun(context, y, runStart, right + 1, runSegment, hoverProgress, openProgress);
        }
    }

    private void drawRun(UiContext context, int y, int start, int endExclusive,
                         int segmentIndex, double[] hoverProgress, double openProgress) {
        if (segmentIndex < 0 || endExclusive <= start) return;
        RadialSegment segment = segments.get(segmentIndex);
        UiPalette palette = context.theme().palette();
        int baseColor = (segmentIndex & 1) == 0 ? palette.card() : palette.button();
        int color = !segment.enabled() ? palette.disabled()
                : UiPaint.mixColor(baseColor, palette.accent(), hoverProgress[segmentIndex]);
        UiPaint.rect(context, new Bounds(start, y, endExclusive - start, 1),
                UiPaint.withAlpha(color, openProgress));
        if(context.theme().skin().pixelated()){
            UiPaint.rect(context,new Bounds(start,y,1,1),WorkshopSkin.EDGE_LIGHT);
            UiPaint.rect(context,new Bounds(endExclusive-1,y,1,1),WorkshopSkin.EDGE_DARK);
        }
    }

    private void drawCenter(UiContext context, RadialGeometry.Point center, double innerRadius,
                            double openProgress) {
        int radius = Math.max(1, (int) Math.floor(innerRadius));
        int top = Math.max(bounds().y(), (int) Math.ceil(center.y() - radius));
        int bottom = Math.min(bounds().bottom() - 1, (int) Math.floor(center.y() + radius));
        for (int y = top; y <= bottom; y++) {
            double dy = y + 0.5 - center.y();
            int span = (int) Math.floor(Math.sqrt(Math.max(0, radius * radius - dy * dy)));
            UiPaint.rect(context, new Bounds((int) Math.round(center.x()) - span, y,
                    span * 2 + 1, 1), UiPaint.withAlpha(context.theme().palette().panel(), openProgress));
        }
    }

    private void drawSegmentContents(UiContext context, RadialGeometry.Point center,
                                     double innerRadius, double outerRadius, double openProgress) {
        double contentRadius = (innerRadius + outerRadius) * 0.55;
        for (int index = 0; index < segments.size(); index++) {
            RadialSegment segment = segments.get(index);
            double angle = RadialGeometry.segmentCenterAngle(index, segments.size());
            int contentX = (int) Math.round(center.x() + Math.cos(angle) * contentRadius);
            int contentY = (int) Math.round(center.y() + Math.sin(angle) * contentRadius);
            UiPalette palette = context.theme().palette();
            int color = segment.enabled() ? palette.text() : palette.mutedText();
            ItemStack icon = segment.icon();
            int labelY = contentY - UiText.height(context) / 2;
            if (!icon.isEmpty()) {
                int iconY = contentY - 10;
                context.graphics().pose().pushPose();
                float iconScale = (float) (0.82 + 0.18 * openProgress);
                context.graphics().pose().translate(contentX, iconY + 8, 0);
                context.graphics().pose().scale(iconScale, iconScale, 1);
                context.graphics().pose().translate(-contentX, -iconY - 8, 0);
                context.graphics().renderItem(icon, contentX - 8, iconY);
                context.graphics().pose().popPose();
                labelY = contentY + 10;
            }
            String label = segment.label().getString();
            int maxLabelWidth = Math.max(8, (int) Math.floor(outerRadius * 0.72));
            if (UiText.width(context.font(), label, context.theme()) > maxLabelWidth) {
                while (label.length() > 1 && UiText.width(context.font(), label + "…", context.theme()) > maxLabelWidth) {
                    label = label.substring(0, label.length() - 1);
                }
                label += "…";
            }
            UiPaint.textCentered(context.withOpacity(openProgress), label, contentX, labelY, color);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!open || !isEnabled() || button != 0 || segments.isEmpty()
                || !containsWithinRadius(mouseX, mouseY)) return false;
        gestureActive = true;
        updateHoveredSegment(mouseX, mouseY);
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!gestureActive || button != 0) return false;
        updateHoveredSegment(mouseX, mouseY);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!gestureActive || button != 0) return false;
        updateHoveredSegment(mouseX, mouseY);
        int selected = hoveredSegment;
        gestureActive = false;
        hoveredSegment = -1;
        if (open && isEnabled() && selected >= 0) {
            RadialSegment segment = segments.get(selected);
            if (segment.enabled()) segment.onSelect().run();
        }
        return true;
    }

    public void cancelGesture() {
        gestureActive = false;
        hoveredSegment = -1;
    }

    private void updateHoveredSegment(double mouseX, double mouseY) {
        if (!containsWithinRadius(mouseX, mouseY)) {
            hoveredSegment = -1;
            return;
        }
        hoveredSegment = RadialGeometry.segmentAt(mouseX, mouseY, RadialGeometry.center(bounds()),
                RadialGeometry.innerRadius(RadialGeometry.outerRadius(bounds()), DEAD_ZONE_FRACTION),
                RadialGeometry.outerRadius(bounds()), segments.size());
    }

    private boolean containsWithinRadius(double x, double y) {
        if (!bounds().contains(x, y)) return false;
        RadialGeometry.Point center = RadialGeometry.center(bounds());
        double radius = RadialGeometry.outerRadius(bounds());
        double dx = x - center.x();
        double dy = y - center.y();
        return dx * dx + dy * dy <= radius * radius;
    }

    @Override
    public UiComponent hitTest(double mouseX, double mouseY) {
        return open && isEnabled() && containsWithinRadius(mouseX, mouseY) ? this : null;
    }
}
