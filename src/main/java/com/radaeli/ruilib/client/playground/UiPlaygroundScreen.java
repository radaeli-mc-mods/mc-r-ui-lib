package com.radaeli.ruilib.client.playground;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.radaeli.ruilib.ui.AbsoluteLayout;
import com.radaeli.ruilib.ui.Alignment;
import com.radaeli.ruilib.ui.Bounds;
import com.radaeli.ruilib.ui.Tooltip;
import com.radaeli.ruilib.ui.Button;
import com.radaeli.ruilib.ui.Column;
import com.radaeli.ruilib.ui.Label;
import com.radaeli.ruilib.ui.Modal;
import com.radaeli.ruilib.ui.ProgressBar;
import com.radaeli.ruilib.ui.Row;
import com.radaeli.ruilib.ui.Slider;
import com.radaeli.ruilib.ui.UiComponent;
import com.radaeli.ruilib.ui.UiContext;
import com.radaeli.ruilib.ui.UiPaint;
import com.radaeli.ruilib.ui.UiScreen;
import com.radaeli.ruilib.ui.UiTheme;
import com.radaeli.ruilib.ui.UiTextStyle;
import com.radaeli.ruilib.ui.UiThemes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Development-only visual editor for composing and exercising toolkit components. */
public final class UiPlaygroundScreen extends UiScreen {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int SIDEBAR_WIDTH = 98;
    private static final int INSPECTOR_WIDTH = 168;
    private static final int TOOLBAR_HEIGHT = 28;
    private static final int PALETTE_BOTTOM_PADDING = 12;
    private static final int PROGRESS_PREVIEW_CYCLE_TICKS = 120;
    private static final String BUILT_IN_LAYOUT_DIRECTORY = "/assets/r_ui_lib/playground/layouts/";
    private static final List<BuiltInLayout> BUILT_IN_LAYOUTS = List.of(
            new BuiltInLayout("basic", "Basic — Basic components"),
            new BuiltInLayout("input", "Input — Input components"),
            new BuiltInLayout("layout", "Layout — Layout components"),
            new BuiltInLayout("data", "Data — Data components"),
            new BuiltInLayout("status", "Status — Status components"),
            new BuiltInLayout("scroll", "Scroll — Scroll components"));
    private static final List<PaletteCategory> PALETTE_CATEGORIES = List.of(
            new PaletteCategory("Basic", List.of("label", "button", "iconbutton", "toggle", "chip", "radio")),
            new PaletteCategory("Input", List.of("textfield", "searchfield", "slider", "stepper", "segmented", "radiogroup", "colorswatch", "colorpicker")),
            new PaletteCategory("Layout", List.of("panel", "card", "divider", "row", "column", "stack",
                    "absolute", "grid", "spacer")),
            new PaletteCategory("Data", List.of("selectablecard", "itemcard", "itemslot", "itemgrid", "table", "tabs", "itempreview")),
            new PaletteCategory("Status", List.of("progressbar", "radial")),
            new PaletteCategory("Scroll", List.of("scroll", "clip", "hscroll")));

    private PlaygroundDocument document = PlaygroundDocument.empty("untitled");
    private PlaygroundNode selected;
    private final AbsoluteLayout canvas;
    private Map<PlaygroundNode, UiComponent> componentMap = PlaygroundComponentFactory.newComponentMap();
    private UiComponent focusedPreviewComponent;
    private UiComponent pressedPreviewComponent;
    private String status = "Arraste componentes da paleta para o canvas.";
    private int clickCount;
    private boolean previewMode;
    private int paletteScrollOffset;
    private final Set<String> collapsedPaletteCategories = new LinkedHashSet<>();
    private int previewPanX;
    private int previewPanY;
    private int lastPanX;
    private int lastPanY;
    private boolean panningPreview;
    private int progressPreviewTicks;

    private int sidebarWidth;
    private int inspectorWidth;
    private int canvasX;
    private int canvasY;
    private int canvasWidth;
    private int canvasHeight;

    private String paletteDragType;
    private PlaygroundNode draggingNode;
    private int dragOffsetX;
    private int dragOffsetY;
    private int lastMouseX;
    private int lastMouseY;
    private boolean hasDragged;
    private boolean resizing;

    private EditBox nameField;
    private EditBox textField;
    private EditBox colorField;
    private EditBox xField;
    private EditBox yField;
    private EditBox widthField;
    private EditBox heightField;
    private EditBox gapField;
    private EditBox rangeField;
    private EditBox sliderValueField;
    private EditBox sliderUpperValueField;
    private EditBox itemSlotBackgroundField;
    private final List<AbstractWidget> inspectorWidgets = new ArrayList<>();
    private net.minecraft.client.gui.components.Button horizontalAlignmentButton;
    private net.minecraft.client.gui.components.Button verticalAlignmentButton;
    private net.minecraft.client.gui.components.Button enabledButton;

    public UiPlaygroundScreen() {
        this(new AbsoluteLayout());
    }

    private UiPlaygroundScreen(AbsoluteLayout canvas) {
        super(Component.literal("UI Toolkit Playground"), canvas);
        this.canvas = canvas;
        loadLastSavedDocument();
        rebuildCanvas();
    }

    @Override
    protected void init() {
        super.init();
        int width = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int height = Minecraft.getInstance().getWindow().getGuiScaledHeight();

        updateCanvasLayout(width, height);

        boolean compactToolbar = width < 300;
        int nameWidth = Math.min(78, Math.max(compactToolbar ? 36 : 62, width / 5));
        int cursorX = 6;
        nameField = addRenderableWidget(new EditBox(font, cursorX, 5, nameWidth, 20, Component.literal("Layout name")));
        nameField.setMaxLength(40);
        nameField.setValue(document.name);
        cursorX += nameWidth + 3;
        int topButtonWidth = Math.max(20, (width - cursorX - 24) / 8);
        String newLabel = compactToolbar ? "N" : "New";
        String saveLabel = compactToolbar ? "S" : "Save";
        String loadLabel = compactToolbar ? "L" : "Load";
        String modeLabel = compactToolbar ? (previewMode ? "E" : "P") : (previewMode ? "Edit" : "Preview");
        String closeLabel = compactToolbar ? "X" : "Close";
        cursorX = addTopButton(cursorX, newLabel, topButtonWidth, this::newDocument);
        cursorX = addTopButton(cursorX, saveLabel, topButtonWidth, this::saveDocument);
        cursorX = addTopButton(cursorX, loadLabel, topButtonWidth, this::openLoadPicker);
        cursorX = addTopButton(cursorX, modeLabel, topButtonWidth, this::toggleMode);
        String toastLabel = compactToolbar ? "T" : "Toast";
        cursorX = addTopButton(cursorX, toastLabel, topButtonWidth, this::showToastDemo);
        String modalLabel = compactToolbar ? "D" : "Dialog";
        cursorX = addTopButton(cursorX, modalLabel, topButtonWidth, this::showModalDemo);
        String themeLabel = compactToolbar ? "Th" : "Theme";
        cursorX = addTopButton(cursorX, themeLabel, topButtonWidth, this::openThemePicker);
        addTopButton(cursorX, closeLabel, topButtonWidth, this::onClose);

        inspectorWidgets.clear();
        createInspectorWidgets(width);
        refreshInspector();
    }

    private void updateCanvasLayout(int screenWidth, int screenHeight) {
        if (previewMode) {
            sidebarWidth = 0;
            inspectorWidth = 0;
            canvasX = 8;
            canvasWidth = Math.max(28, screenWidth - 16);
        } else {
            sidebarWidth = screenWidth < 470 ? 76 : SIDEBAR_WIDTH;
            inspectorWidth = screenWidth < 470 ? 118 : INSPECTOR_WIDTH;
            if (screenWidth < 300) {
                sidebarWidth = 58;
                inspectorWidth = 94;
            }
            canvasX = sidebarWidth + 8;
            canvasWidth = Math.max(28, screenWidth - sidebarWidth - inspectorWidth - 24);
        }
        canvasY = TOOLBAR_HEIGHT + 8;
        canvasHeight = Math.max(40, screenHeight - TOOLBAR_HEIGHT - 24);
        paletteScrollOffset = Math.min(paletteScrollOffset, maxPaletteScrollOffset());
        root().layout(font, theme(), new Bounds(canvasX, canvasY, canvasWidth, canvasHeight));
    }

    @Override
    protected void layoutRoot(UiTheme activeTheme) {
        root().layout(font, activeTheme, new Bounds(canvasX, canvasY, canvasWidth, canvasHeight));
    }

    @Override
    public void tick() {
        super.tick();
        if (nameField != null) com.radaeli.ruilib.platform.UiPlatform.tickEditor(nameField);
        for (var widget : inspectorWidgets) {
            if (widget instanceof EditBox editor) com.radaeli.ruilib.platform.UiPlatform.tickEditor(editor);
        }
        if (previewMode) {
            progressPreviewTicks++;
            // Feed the tween every client tick so the theme's transition overlaps updates
            // instead of completing between samples and visibly pausing.
            updatePreviewProgressBars(progressPreviewRatio());
        }
    }

    private double progressPreviewRatio() {
        double phase = (progressPreviewTicks % PROGRESS_PREVIEW_CYCLE_TICKS)
                / (double) PROGRESS_PREVIEW_CYCLE_TICKS;
        return phase <= 0.5 ? phase * 2 : (1 - phase) * 2;
    }

    private void updatePreviewProgressBars(double ratio) {
        for (Map.Entry<PlaygroundNode, UiComponent> entry : componentMap.entrySet()) {
            if (entry.getValue() instanceof ProgressBar progressBar) {
                PlaygroundNode node = entry.getKey();
                progressBar.setValue(node.minimum + (node.maximum - node.minimum) * ratio);
            }
        }
    }

    private int addTopButton(int x, String label, int buttonWidth, Runnable action) {
        addRenderableWidget(net.minecraft.client.gui.components.Button.builder(
                Component.literal(label), button -> action.run())
                .bounds(x, 5, buttonWidth, 20).build());
        return x + buttonWidth + 3;
    }

    private void showToastDemo() {
        UiPlaygroundToastDemo.dispatch(toastHost());
    }

    private void showModalDemo() {
        clearPreviewFocus();
        pressedPreviewComponent = null;
        Column content = new Column(10);
        content.add(new Label(Component.literal("Modal overlay with contained focus.")));
        Row actions = new Row(6);
        actions.add(new Button(Component.literal("Close"), this::closeModal));
        content.add(actions);
        showModal(new Modal(Component.literal("Presentation layer"), content, 240, 112)
                .setCloseOnOutsideClick(true));
    }

    private void openThemePicker() {
        Column content = new Column(8);
        content.add(new Label(Component.literal("Choose the global UI theme")));
        Row choices = new Row(8);
        boolean darkSelected = theme().equals(UiTheme.DARK);
        boolean lightSelected = theme().equals(UiTheme.LIGHT);
        choices.add(new Button(Component.literal(darkSelected ? "Dark · Active" : "Dark"),
                () -> selectGlobalTheme(UiTheme.DARK)));
        choices.add(new Button(Component.literal(lightSelected ? "Light · Active" : "Light"),
                () -> selectGlobalTheme(UiTheme.LIGHT)));
        content.add(choices);
        showModal(new Modal(Component.literal("Global theme"), content, 240, 104)
                .setCloseOnOutsideClick(true));
    }

    private void selectGlobalTheme(UiTheme selectedTheme) {
        UiThemes.setGlobalTheme(selectedTheme);
        status = selectedTheme.equals(UiTheme.LIGHT)
                ? "Global UI theme set to Light."
                : "Global UI theme set to Dark.";
        closeModal();
    }

    private void createInspectorWidgets(int screenWidth) {
        int editorInspectorWidth = screenWidth < 300 ? 94 : screenWidth < 470 ? 118 : INSPECTOR_WIDTH;
        int panelX = screenWidth - editorInspectorWidth + 5;
        int fieldWidth = Math.max(34, editorInspectorWidth - 11);
        textField = createField(panelX, 58, fieldWidth, "Component text");
        colorField = createField(panelX, 78, fieldWidth, "ARGB color");
        itemSlotBackgroundField = createField(panelX, 98, fieldWidth, "theme, color name or #HEX");
        int halfWidth = Math.max(32, (fieldWidth - 5) / 2);
        xField = createField(panelX, 98, halfWidth, "X");
        yField = createField(panelX + halfWidth + 5, 98, halfWidth, "Y");
        widthField = createField(panelX, 118, halfWidth, "Width");
        heightField = createField(panelX + halfWidth + 5, 118, halfWidth, "Height");
        gapField = createField(panelX, 138, fieldWidth, "Gap");
        rangeField = createField(panelX, 160, fieldWidth, "Min, max, step");
        sliderValueField = createField(panelX, 182, halfWidth, "Slider value / lower endpoint");
        sliderUpperValueField = createField(panelX + halfWidth + 5, 182, halfWidth, "Upper endpoint (optional)");
        sliderUpperValueField.setHint(Component.literal("Optional"));
        int alignmentWidth = Math.max(32, (fieldWidth - 4) / 2);
        horizontalAlignmentButton = addInspectorButton(net.minecraft.client.gui.components.Button.builder(
                Component.literal("H"), button -> cycleAlignment(true))
                .bounds(panelX, 160, alignmentWidth, 18).build());
        verticalAlignmentButton = addInspectorButton(net.minecraft.client.gui.components.Button.builder(
                Component.literal("V"), button -> cycleAlignment(false))
                .bounds(panelX + alignmentWidth + 4, 160, alignmentWidth, 18).build());
        enabledButton = addInspectorButton(net.minecraft.client.gui.components.Button.builder(
                Component.literal("Enabled"), button -> toggleEnabled())
                .bounds(panelX, 182, fieldWidth, 18).build());
        int actionY = Math.max(204, height - 38);
        addInspectorButton(net.minecraft.client.gui.components.Button.builder(
                Component.literal("Apply"), button -> applyInspector())
                .bounds(panelX, actionY, alignmentWidth, 18).build());
        addInspectorButton(net.minecraft.client.gui.components.Button.builder(
                Component.literal("Remove"), button -> removeSelected())
                .bounds(panelX + alignmentWidth + 4, actionY, alignmentWidth, 18).build());
    }

    private net.minecraft.client.gui.components.Button addInspectorButton(
            net.minecraft.client.gui.components.Button button) {
        inspectorWidgets.add(button);
        return addRenderableWidget(button);
    }

    private EditBox createField(int x, int y, int fieldWidth, String hint) {
        EditBox field = new EditBox(font, x, y, fieldWidth, 18, Component.literal(hint));
        field.setMaxLength(128);
        inspectorWidgets.add(field);
        return addRenderableWidget(field);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        ensureThemeLayout();
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        UiContext context = new UiContext(graphics, font, theme(), mouseX, mouseY, width, height);
        drawEditorPanels(context);
        graphics.enableScissor(canvasX, canvasY, canvasX + canvasWidth, canvasY + canvasHeight);
        drawCanvasGrid(graphics);
        root().render(context);
        drawCanvasDebug(context);
        if (paletteDragType != null && inCanvas(mouseX, mouseY)) {
            UiPaint.roundedOutline(context, new Bounds(mouseX - 18, mouseY - 10, 72, 22),
                    theme().palette().focus(), theme().tokens().radii().small());
            UiPaint.text(context, paletteDragType, mouseX - 14, mouseY - 5, theme().palette().text());
        }
        if (previewMode) {
            UiPaint.text(context, "Preview · clicks: " + clickCount + " · pan: middle-drag",
                    canvasX + 4, canvasY + 4, theme().palette().mutedText(), UiTextStyle.SMALL);
        }
        graphics.disableScissor();
        renderToastOverlay(graphics, mouseX, mouseY);
        UiPaint.text(context, status, 8, height - 12, theme().palette().mutedText(), UiTextStyle.SMALL);
        renderables.forEach(renderable -> renderable.render(graphics, mouseX, mouseY, partialTick));
        renderClosingModal(context);
        if (modal() != null) modal().render(context);
        renderCarriedItem(graphics, mouseX, mouseY);
        UiComponent hovered = (modal() == null ? root() : modal()).hitTest(mouseX, mouseY);
        Tooltip hoveredTooltip = hovered == null ? null : hovered.tooltipAt(mouseX, mouseY);
        if (hoveredTooltip != null) {
            UiPaint.tooltip(context, hoveredTooltip, mouseX, mouseY);
        }
    }

    private void drawCanvasGrid(GuiGraphics graphics) {
        int gridOffsetX = Math.floorMod(previewPanX, 16);
        int gridOffsetY = Math.floorMod(previewPanY, 16);
        for (int x = canvasX + gridOffsetX + 16; x < canvasX + canvasWidth; x += 16) {
            graphics.fill(x, canvasY, x + 1, canvasY + canvasHeight,
                    UiPaint.withAlpha(theme().palette().border(), 0.12));
        }
        for (int y = canvasY + gridOffsetY + 16; y < canvasY + canvasHeight; y += 16) {
            graphics.fill(canvasX, y, canvasX + canvasWidth, y + 1,
                    UiPaint.withAlpha(theme().palette().border(), 0.12));
        }
    }

    private void drawEditorPanels(UiContext context) {
        GuiGraphics graphics = context.graphics();
        UiPaint.rect(context, new Bounds(0, 0, width, height), theme().palette().screen());
        UiPaint.rect(context, new Bounds(0, 0, width, TOOLBAR_HEIGHT), theme().palette().panel());
        if (previewMode) {
            UiPaint.panel(context, new Bounds(canvasX, canvasY, canvasWidth, canvasHeight));
            return;
        }
        UiPaint.panel(context, new Bounds(4, canvasY, sidebarWidth, canvasHeight));
        UiPaint.panel(context, new Bounds(canvasX, canvasY, canvasWidth, canvasHeight));
        UiPaint.panel(context, new Bounds(width - inspectorWidth + 1, canvasY, inspectorWidth - 5, canvasHeight));
        UiPaint.text(context, "COMPONENTS", 8, canvasY + 7, theme().palette().mutedText());
        UiPaint.text(context, "INSPECTOR", width - inspectorWidth + 7, canvasY + 7, theme().palette().mutedText());
        int paletteTop = canvasY + 22;
        int paletteBottom = canvasY + canvasHeight - PALETTE_BOTTOM_PADDING;
        graphics.enableScissor(8, paletteTop, sidebarWidth, paletteBottom);
        int first = paletteScrollOffset;
        List<PaletteEntry> entries = paletteEntries();
        int last = Math.min(entries.size(), first + visiblePaletteSlots());
        for (int i = first; i < last; i++) {
            int y = paletteTop + (i - first) * 20;
            PaletteEntry entry = entries.get(i);
            boolean categoryRow = entry.type == null;
            boolean collapsed = collapsedPaletteCategories.contains(entry.category);
            int color = !categoryRow && paletteDragType != null && paletteDragType.equals(entry.type)
                    ? theme().palette().buttonHover()
                    : categoryRow ? theme().palette().panel() : theme().palette().button();
            Bounds entryBounds = new Bounds(8, y, sidebarWidth - 8, 17);
            UiPaint.roundedRect(context, entryBounds, color, theme().tokens().radii().small());
            if (categoryRow) {
                UiPaint.text(context, (collapsed ? "> " : "v ") + entry.category,
                        11, y + 5, theme().palette().accent(), UiTextStyle.SMALL);
            } else {
                UiPaint.roundedOutline(context, entryBounds, theme().palette().border(),
                        theme().tokens().radii().small());
                UiPaint.text(context, entry.type, 17, y + 5, theme().palette().text(), UiTextStyle.SMALL);
            }
        }
        graphics.disableScissor();
        drawInspectorLabels(context, width - inspectorWidth + 7);
    }

    private void drawInspectorLabels(UiContext context, int x) {
        int labelColor = theme().palette().mutedText();
        if (textField.visible) UiPaint.text(context, textLabel(), x, textField.getY() - 10, labelColor, UiTextStyle.SMALL);
        if (colorField.visible) UiPaint.text(context, selected != null && selected.usesItemStack()
                ? "Item ID" : "Color ARGB", x, colorField.getY() - 10, labelColor, UiTextStyle.SMALL);
        if (itemSlotBackgroundField.visible) UiPaint.text(context, "Background color",
                x, itemSlotBackgroundField.getY() - 10, labelColor, UiTextStyle.SMALL);
        if (xField.visible || yField.visible) {
            int pairX = x + Math.max(36, (inspectorWidth - 16) / 2) + 5;
            UiPaint.text(context, "X", x, xField.getY() - 10, labelColor, UiTextStyle.SMALL);
            UiPaint.text(context, "Y", pairX, yField.getY() - 10, labelColor, UiTextStyle.SMALL);
        }
        if (widthField.visible || heightField.visible) {
            int pairX = x + Math.max(36, (inspectorWidth - 16) / 2) + 5;
            UiPaint.text(context, "Width", x, widthField.getY() - 10, labelColor, UiTextStyle.SMALL);
            UiPaint.text(context, "Height", pairX, heightField.getY() - 10, labelColor, UiTextStyle.SMALL);
        }
        if (gapField.visible) UiPaint.text(context, "Gap", x, gapField.getY() - 10, labelColor, UiTextStyle.SMALL);
        if (rangeField.visible) UiPaint.text(context, rangeLabel(), x,
                rangeField.getY() - 10, labelColor, UiTextStyle.SMALL);
        if (sliderValueField.visible) {
            UiPaint.text(context, "Value", x, sliderValueField.getY() - 10, labelColor, UiTextStyle.SMALL);
            UiPaint.text(context, "Upper", sliderUpperValueField.getX(),
                    sliderUpperValueField.getY() - 10, labelColor, UiTextStyle.SMALL);
        }
        if (selected != null) {
            UiPaint.text(context, selected.type.toUpperCase(), x, canvasY + 22,
                    theme().palette().accent(), UiTextStyle.HEADING);
        } else {
            UiPaint.text(context, "No selection", x, canvasY + 22,
                    theme().palette().mutedText(), UiTextStyle.HEADING);
        }
    }

    private String rangeLabel() {
        if (selected == null) return "Value";
        return selected.type.equals("slider") ? "Min, max, step"
                : selected.type.equals("progressbar") ? "Min, max, value"
                : selected.supportsItemCount() ? "Stack count"
                : selected.type.equals("table") ? "Table columns"
                : selected.supportsColumns() ? "Grid columns" : "Value";
    }

    private String textLabel() {
        if (selected == null) return "Text";
        if (selected.type.equals("itemcard")) return "Item label";
        if (selected.type.equals("table")) return "Headers (CSV)";
        if (selected.type.equals("radial")) return "Segments (CSV)";
        return "Text / label";
    }

    private void drawCanvasDebug(UiContext context) {
        if (previewMode) return;
        UiComponent rootComponent = root();
        UiPaint.roundedOutline(context, rootComponent.bounds(), theme().palette().border(),
                theme().tokens().radii().large());
        for (Map.Entry<PlaygroundNode, UiComponent> entry : componentMap.entrySet()) {
            UiComponent component = entry.getValue();
            Bounds bounds = component.bounds();
            UiPaint.roundedOutline(context, bounds, entry.getKey() == selected
                    ? theme().palette().focus() : UiPaint.withAlpha(theme().palette().mutedText(), 0.5),
                    theme().tokens().radii().medium());
        }
        UiComponent selectedComponent = selected == null ? null : componentMap.get(selected);
        if (!previewMode && selectedComponent != null) {
            Bounds bounds = selectedComponent.bounds();
            UiPaint.roundedRect(context, new Bounds(bounds.right() - 5, bounds.bottom() - 5, 8, 8),
                    theme().palette().focus(), theme().tokens().radii().small());
            String info = (bounds.x() - canvasX) + ", " + (bounds.y() - canvasY)
                    + "  " + bounds.width() + "×" + bounds.height();
            UiPaint.text(context, info, bounds.x(), Math.max(canvasY, bounds.y() - 10),
                    theme().palette().focus(), UiTextStyle.SMALL);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (modal() != null) return super.mouseClicked(mouseX, mouseY, button);
        if (handleToastClick(mouseX, mouseY, button)) {
            clearPreviewFocus();
            pressedPreviewComponent = null;
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE && inCanvas(mouseX, mouseY)) {
            panningPreview = true;
            lastPanX = (int) mouseX;
            lastPanY = (int) mouseY;
            pressedPreviewComponent = null;
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && !previewMode && inPalette(mouseX, mouseY)) {
            setFocused(null);
            setDragging(false);
            int paletteIndex = paletteScrollOffset + (int) ((mouseY - canvasY - 22) / 20);
            List<PaletteEntry> entries = paletteEntries();
            if (paletteIndex >= 0 && paletteIndex < entries.size()) {
                PaletteEntry entry = entries.get(paletteIndex);
                if (entry.type == null) {
                    if (!collapsedPaletteCategories.add(entry.category)) {
                        collapsedPaletteCategories.remove(entry.category);
                    }
                    paletteScrollOffset = Math.min(paletteScrollOffset, maxPaletteScrollOffset());
                    return true;
                }
                paletteDragType = entry.type;
                lastMouseX = (int) mouseX;
                lastMouseY = (int) mouseY;
                return true;
            }
        }
        if (inCanvas(mouseX, mouseY)) {
            setFocused(null);
            setDragging(false);
            if (previewMode) return previewMouseClicked(mouseX, mouseY, button);
            return editorMouseClicked(mouseX, mouseY, button);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean editorMouseClicked(double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return true;
        PlaygroundNode hit = nodeAt(mouseX, mouseY);
        if (hit == null) {
            selected = null;
            refreshInspector();
            return true;
        }
        selected = hit;
        UiComponent component = componentMap.get(hit);
        Bounds bounds = component.bounds();
        resizing = mouseX >= bounds.right() - 7 && mouseY >= bounds.bottom() - 7;
        draggingNode = hit;
        hasDragged = false;
        dragOffsetX = (int) mouseX - bounds.x();
        dragOffsetY = (int) mouseY - bounds.y();
        lastMouseX = (int) mouseX;
        lastMouseY = (int) mouseY;
        refreshInspector();
        return true;
    }

    private boolean previewMouseClicked(double mouseX, double mouseY, int button) {
        UiComponent target = canvas.hitTest(mouseX, mouseY);
        clearPreviewFocus();
        if (target == null) {
            pressedPreviewComponent = null;
            return true;
        }
        if (target.isFocusable()) {
            focusedPreviewComponent = target;
            focusedPreviewComponent.setFocused(true);
        }
        pressedPreviewComponent = target.mouseClicked(mouseX, mouseY, button) ? target : null;
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (modal() != null) return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        if (panningPreview && button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
            int currentX = (int) mouseX;
            int currentY = (int) mouseY;
            applyPreviewPan(currentX - lastPanX, currentY - lastPanY);
            lastPanX = currentX;
            lastPanY = currentY;
            return true;
        }
        if (previewMode && pressedPreviewComponent != null
                && pressedPreviewComponent.mouseDragged(mouseX, mouseY, button, dragX, dragY)) return true;
        if (paletteDragType != null || draggingNode != null) {
            if (Math.abs((int) mouseX - lastMouseX) + Math.abs((int) mouseY - lastMouseY) > 1) {
                hasDragged = true;
            }
            if (paletteDragType == null && selected != null) {
                if (resizing) {
                    int deltaWidth = (int) mouseX - lastMouseX;
                    int deltaHeight = (int) mouseY - lastMouseY;
                    selected.width = Math.max(12, Math.min(8192, selected.width + deltaWidth));
                    selected.height = Math.max(selected.type.equals("divider") ? 1 : 12,
                            Math.min(8192, selected.height + deltaHeight));
                } else if (selected.parent != null && selected.parent.isAbsolute()) {
                    Bounds parentBounds = componentMap.get(selected.parent).bounds();
                    int parentPanX = selected.parent == document.root ? previewPanX : 0;
                    int parentPanY = selected.parent == document.root ? previewPanY : 0;
                    selected.x = clamp((int) mouseX - parentBounds.x() - parentPanX - dragOffsetX,
                            -100_000, 100_000);
                    selected.y = clamp((int) mouseY - parentBounds.y() - parentPanY - dragOffsetY,
                            -100_000, 100_000);
                }
                rebuildCanvas();
            }
            lastMouseX = (int) mouseX;
            lastMouseY = (int) mouseY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (modal() != null) return super.mouseReleased(mouseX, mouseY, button);
        if (panningPreview && button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
            panningPreview = false;
            return true;
        }
        if (paletteDragType != null) {
            if (inCanvas(mouseX, mouseY)) {
                addNode(paletteDragType, (int) mouseX, (int) mouseY);
            }
            paletteDragType = null;
            return true;
        }
        if (draggingNode != null) {
            if (!resizing && hasDragged) {
                PlaygroundNode destination = dropContainerAt(mouseX, mouseY, draggingNode);
                if (destination == null && inCanvas(mouseX, mouseY)) destination = document.root;
                if (destination != null && destination != draggingNode && !draggingNode.contains(destination)) {
                    reparent(draggingNode, destination, (int) mouseX, (int) mouseY);
                }
            }
            draggingNode = null;
            resizing = false;
            hasDragged = false;
            refreshInspector();
            return true;
        }
        if (pressedPreviewComponent != null) {
            UiComponent target = pressedPreviewComponent;
            pressedPreviewComponent = null;
            target.mouseReleased(mouseX, mouseY, button);
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (modal() != null) return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        if (!previewMode && inPalette(mouseX, mouseY) && scrollY != 0) {
            paletteScrollOffset = clamp(paletteScrollOffset - (int) Math.signum(scrollY),
                    0, maxPaletteScrollOffset());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (modal() != null) return super.keyPressed(keyCode, scanCode, modifiers);
        if (previewMode) {
            if (keyCode == GLFW.GLFW_KEY_TAB) {
                movePreviewFocus((modifiers & GLFW.GLFW_MOD_SHIFT) != 0);
                return true;
            }
            if (focusedPreviewComponent != null
                    && focusedPreviewComponent.keyPressed(keyCode, scanCode, modifiers)) return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DELETE && !(getFocused() instanceof EditBox)
                && !previewMode && selected != null) {
            removeSelected();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (modal() != null) return super.charTyped(codePoint, modifiers);
        if (previewMode && focusedPreviewComponent != null
                && focusedPreviewComponent.charTyped(codePoint, modifiers)) return true;
        return super.charTyped(codePoint, modifiers);
    }

    private void movePreviewFocus(boolean backwards) {
        List<UiComponent> focusable = new ArrayList<>();
        canvas.visitFocusable(focusable::add);
        if (focusable.isEmpty()) {
            clearPreviewFocus();
            return;
        }
        int current = focusable.indexOf(focusedPreviewComponent);
        int next = backwards ? (current <= 0 ? focusable.size() - 1 : current - 1)
                : (current + 1) % focusable.size();
        clearPreviewFocus();
        focusedPreviewComponent = focusable.get(next);
        focusedPreviewComponent.setFocused(true);
    }

    private void clearPreviewFocus() {
        if (focusedPreviewComponent != null) focusedPreviewComponent.setFocused(false);
        focusedPreviewComponent = null;
    }

    private void addNode(String type, int mouseX, int mouseY) {
        PlaygroundNode parent = dropContainerAt(mouseX, mouseY, null);
        if (parent == null) parent = document.root;
        Bounds parentBounds = componentMap.get(parent).bounds();
        int parentPanX = parent == document.root ? previewPanX : 0;
        int parentPanY = parent == document.root ? previewPanY : 0;
        PlaygroundNode node = PlaygroundNode.create(type,
                mouseX - parentBounds.x() - parentPanX - 24,
                mouseY - parentBounds.y() - parentPanY - 12);
        if (!parent.isAbsolute()) {
            node.x = 0;
            node.y = 0;
        }
        parent.addChild(node, parent.children.size());
        selected = node;
        rebuildCanvas();
        refreshInspector();
        status = "Adicionado: " + node.type;
    }

    private void reparent(PlaygroundNode node, PlaygroundNode newParent, int mouseX, int mouseY) {
        if (!newParent.isContainer() || node == document.root) return;
        PlaygroundNode oldParent = node.parent;
        if (oldParent == newParent && newParent.isAbsolute()) return;
        if (oldParent == null || !oldParent.removeChild(node)) return;
        if (newParent.isAbsolute()) {
            Bounds targetBounds = componentMap.get(newParent).bounds();
            int parentPanX = newParent == document.root ? previewPanX : 0;
            int parentPanY = newParent == document.root ? previewPanY : 0;
            node.x = mouseX - targetBounds.x() - parentPanX - componentMap.get(node).bounds().width() / 2;
            node.y = mouseY - targetBounds.y() - parentPanY - componentMap.get(node).bounds().height() / 2;
        } else {
            node.x = 0;
            node.y = 0;
        }
        newParent.addChild(node, newParent.children.size());
        selected = node;
        rebuildCanvas();
    }

    private PlaygroundNode dropContainerAt(double mouseX, double mouseY, PlaygroundNode ignored) {
        PlaygroundNode hit = nodeAt(document.root, mouseX, mouseY, ignored);
        while (hit != null && !hit.isContainer()) hit = hit.parent;
        return hit;
    }

    private PlaygroundNode nodeAt(double mouseX, double mouseY) {
        return nodeAt(document.root, mouseX, mouseY, null);
    }

    private PlaygroundNode nodeAt(PlaygroundNode node, double mouseX, double mouseY, PlaygroundNode ignored) {
        UiComponent component = componentMap.get(node);
        if (component == null || !component.bounds().contains(mouseX, mouseY)) return null;
        for (int index = node.children.size() - 1; index >= 0; index--) {
            PlaygroundNode childHit = nodeAt(node.children.get(index), mouseX, mouseY, ignored);
            if (childHit != null) return childHit;
        }
        if (node == ignored) return null;
        return node == document.root ? null : node;
    }

    private boolean inCanvas(double mouseX, double mouseY) {
        return mouseX >= canvasX && mouseX < canvasX + canvasWidth
                && mouseY >= canvasY && mouseY < canvasY + canvasHeight;
    }

    private boolean inPalette(double mouseX, double mouseY) {
        return mouseX >= 8 && mouseX < sidebarWidth && mouseY >= canvasY + 22
                && mouseY < canvasY + canvasHeight - PALETTE_BOTTOM_PADDING;
    }

    private int visiblePaletteSlots() {
        int viewportHeight = canvasHeight - 22 - PALETTE_BOTTOM_PADDING;
        return Math.max(1, (viewportHeight - 17) / 20 + 1);
    }

    private int maxPaletteScrollOffset() {
        return Math.max(0, paletteEntries().size() - visiblePaletteSlots());
    }

    private List<PaletteEntry> paletteEntries() {
        List<PaletteEntry> entries = new ArrayList<>();
        for (PaletteCategory category : PALETTE_CATEGORIES) {
            entries.add(new PaletteEntry(category.name, null));
            if (!collapsedPaletteCategories.contains(category.name)) {
                for (String type : category.types) entries.add(new PaletteEntry(category.name, type));
            }
        }
        return entries;
    }

    private record PaletteCategory(String name, List<String> types) { }

    record BuiltInLayout(String id, String label) { }

    private record PaletteEntry(String category, String type) { }

    private void rebuildCanvas() {
        document.normalize();
        componentMap = rebuildIntoStableRoot();
        canvas.layout(font == null ? Minecraft.getInstance().font : font, theme(),
                new Bounds(canvasX, canvasY, Math.max(1, canvasWidth), Math.max(1, canvasHeight)));
    }

    private Map<PlaygroundNode, UiComponent> rebuildIntoStableRoot() {
        Map<PlaygroundNode, UiComponent> rebuiltMap = PlaygroundComponentFactory.newComponentMap();
        AbsoluteLayout rebuiltRoot = (AbsoluteLayout) PlaygroundComponentFactory.build(
                document.root, rebuiltMap, node -> {
                    clickCount++;
                    if (node.type.equals("itemcard")) {
                        if (node.parent != null && node.parent.type.equals("itemgrid")) {
                            for (PlaygroundNode sibling : node.parent.children) {
                                if (sibling.type.equals("itemcard")) sibling.selected = sibling == node;
                            }
                        } else {
                            node.selected = !node.selected;
                        }
                        status = "Item selecionado (" + clickCount + "): " + node.itemId;
                        clearPreviewFocus();
                        rebuildCanvas();
                        refreshInspector();
                    } else if (node.type.equals("itemslot")) {
                        clickCount++;
                        status = node.itemId.isBlank()
                                ? "Filtro de item limpo."
                                : "Filtro de item definido: " + node.itemId;
                        refreshInspector();
                    } else {
                        status = "Ação acionada (" + clickCount + "): " + node.text;
                    }
                }, (node, selected) -> {
                    node.selected = selected;
                    clickCount++;
                    status = node.type.equals("toggle")
                            ? "Toggle " + node.text + (selected ? " ativado" : " desativado")
                            : node.type.equals("radio") ? "Radio " + node.text + " selecionado"
                            : "Chip " + node.text + (selected ? " selecionado" : " desmarcado");
                });
        List<UiComponent> newChildren = new ArrayList<>(rebuiltRoot.children());
        Map<UiComponent, Bounds> newPlacements = new IdentityHashMap<>();
        for (UiComponent child : newChildren) newPlacements.put(child, rebuiltRoot.placementOf(child));
        for (UiComponent child : new ArrayList<>(canvas.children())) canvas.remove(child);
        for (UiComponent child : newChildren) {
            Bounds placement = newPlacements.get(child);
            canvas.addAt(child, new Bounds(placement.x() + previewPanX, placement.y() + previewPanY,
                    placement.width(), placement.height()));
        }
        rebuiltMap.put(document.root, canvas);
        return rebuiltMap;
    }

    private void applyPreviewPan(int deltaX, int deltaY) {
        if (deltaX == 0 && deltaY == 0) return;
        int nextPanX = clamp(previewPanX + deltaX, -100_000, 100_000);
        int nextPanY = clamp(previewPanY + deltaY, -100_000, 100_000);
        int appliedX = nextPanX - previewPanX;
        int appliedY = nextPanY - previewPanY;
        previewPanX = nextPanX;
        previewPanY = nextPanY;
        for (UiComponent child : canvas.children()) {
            Bounds placement = canvas.placementOf(child);
            canvas.setPlacement(child, new Bounds(placement.x() + appliedX, placement.y() + appliedY,
                    placement.width(), placement.height()));
        }
        canvas.layout(font, theme(), new Bounds(canvasX, canvasY, canvasWidth, canvasHeight));
    }

    private void newDocument() {
        document = PlaygroundDocument.empty("untitled");
        selected = null;
        clickCount = 0;
        nameField.setValue(document.name);
        rebuildCanvas();
        refreshInspector();
        status = "Novo layout.";
    }

    private void saveDocument() {
        try {
            document.name = safeName(nameField.getValue());
            document.normalize();
            Path directory = playgroundDirectory();
            Files.createDirectories(directory);
            try (Writer writer = Files.newBufferedWriter(directory.resolve(document.name + ".json"))) {
                GSON.toJson(document, writer);
            }
            try {
                Files.writeString(directory.resolve("last-layout.txt"), document.name, StandardCharsets.UTF_8);
            } catch (IOException exception) {
                status = "Salvo, mas não atualizei o layout padrão: " + shortMessage(exception);
                return;
            }
            status = "Salvo: " + document.name + ".json";
        } catch (IOException | IllegalArgumentException exception) {
            status = "Falha ao salvar: " + shortMessage(exception);
        }
    }

    private void openLoadPicker() {
        Minecraft.getInstance().setScreen(new UiPlaygroundLoadScreen(this, nameField.getValue()));
    }

    boolean loadSavedLayout(String requestedName) {
        try {
            String name = safeName(requestedName);
            Path path = playgroundDirectory().resolve(name + ".json");
            if (!Files.isRegularFile(path)) {
                status = "Arquivo não encontrado: " + name + ".json";
                return false;
            }
            document = readDocument(path);
            document.name = name;
            nameField.setValue(document.name);
            selected = null;
            clickCount = 0;
            rebuildCanvas();
            refreshInspector();
            status = "Carregado: " + document.name + ".json";
            return true;
        } catch (IOException | JsonParseException | IllegalArgumentException exception) {
            status = "Falha ao carregar: " + shortMessage(exception);
            return false;
        }
    }

    static List<BuiltInLayout> builtInLayouts() {
        return BUILT_IN_LAYOUTS;
    }

    boolean loadBuiltInLayout(String layoutId) {
        BuiltInLayout layout = BUILT_IN_LAYOUTS.stream()
                .filter(candidate -> candidate.id().equals(layoutId)).findFirst().orElse(null);
        if (layout == null) {
            status = "Layout de exemplo desconhecido: " + layoutId;
            return false;
        }
        String resource = BUILT_IN_LAYOUT_DIRECTORY + layout.id() + ".json";
        try (var stream = UiPlaygroundScreen.class.getResourceAsStream(resource)) {
            if (stream == null) throw new IOException("Recurso ausente: " + resource);
            try (Reader reader = new java.io.InputStreamReader(stream, StandardCharsets.UTF_8)) {
                document = GSON.fromJson(reader, PlaygroundDocument.class);
            }
            if (document == null) throw new JsonParseException("Documento vazio");
            document.normalize();
            nameField.setValue(document.name);
            selected = null;
            clickCount = 0;
            rebuildCanvas();
            refreshInspector();
            status = "Exemplo carregado: " + layout.label();
            return true;
        } catch (IOException | JsonParseException | IllegalArgumentException exception) {
            status = "Falha ao carregar exemplo: " + shortMessage(exception);
            return false;
        }
    }

    String loadStatus() {
        return status;
    }

    private void loadLastSavedDocument() {
        Path directory = playgroundDirectory();
        try {
            if (!Files.isDirectory(directory)) return;
            List<String> names = savedLayoutNames();
            if (names.isEmpty()) return;

            String lastSaved = null;
            Path pointer = directory.resolve("last-layout.txt");
            if (Files.isRegularFile(pointer)) {
                try {
                    lastSaved = safeName(Files.readString(pointer, StandardCharsets.UTF_8));
                } catch (IOException | IllegalArgumentException ignored) {
                    // A stale or malformed pointer falls back to the most recently modified layout.
                }
            }
            if (lastSaved == null || !names.contains(lastSaved)) {
                lastSaved = names.get(0);
                long newest = Long.MIN_VALUE;
                for (String name : names) {
                    long modified = Files.getLastModifiedTime(directory.resolve(name + ".json")).toMillis();
                    if (modified > newest) {
                        newest = modified;
                        lastSaved = name;
                    }
                }
            }

            document = readDocument(directory.resolve(lastSaved + ".json"));
            document.name = lastSaved;
            status = "Último layout salvo carregado: " + lastSaved + ".json";
        } catch (IOException | JsonParseException | IllegalArgumentException exception) {
            document = PlaygroundDocument.empty("untitled");
            status = "Falha ao carregar último layout: " + shortMessage(exception);
        }
    }

    static List<String> savedLayoutNames() throws IOException {
        Path directory = playgroundDirectory();
        if (!Files.isDirectory(directory)) return List.of();
        try (var paths = Files.list(directory)) {
            return paths.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(fileName -> fileName.toLowerCase(java.util.Locale.ROOT).endsWith(".json"))
                    .map(fileName -> fileName.substring(0, fileName.length() - 5))
                    .filter(name -> name.matches("[A-Za-z0-9_-]+"))
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
        }
    }

    private static PlaygroundDocument readDocument(Path path) throws IOException {
        if (Files.size(path) > 2_000_000) throw new IllegalArgumentException("Arquivo maior que 2 MB");
        try (Reader reader = Files.newBufferedReader(path)) {
            PlaygroundDocument loaded = GSON.fromJson(reader, PlaygroundDocument.class);
            if (loaded == null) throw new JsonParseException("Documento vazio");
            loaded.normalize();
            return loaded;
        }
    }

    private static String safeName(String value) {
        String name = value == null ? "" : value.trim();
        name = name.replaceAll("[^A-Za-z0-9_-]", "_");
        if (name.isBlank() || name.equals(".") || name.equals("..")) {
            throw new IllegalArgumentException("Use um nome simples para o layout");
        }
        return name;
    }

    private static String shortMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null ? exception.getClass().getSimpleName() : message;
    }

    private static Path playgroundDirectory() {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config/r_ui_lib/ui-playground");
    }

    private void toggleMode() {
        previewMode = !previewMode;
        panningPreview = false;
        progressPreviewTicks = 0;
        setFocused(null);
        clearPreviewFocus();
        pressedPreviewComponent = null;
        paletteDragType = null;
        updateCanvasLayout(width, height);
        rebuildCanvas();
        if (previewMode) updatePreviewProgressBars(0);
        refreshInspector();
        modeButtonUpdate();
        for (var renderable : renderables) {
            if (renderable instanceof net.minecraft.client.gui.components.Button button) {
                String label = button.getMessage().getString();
                if (label.equals("Apply") || label.equals("Remove")) button.active = !previewMode;
            }
        }
        status = previewMode ? "Preview: interaja; botão do meio arrasta o canvas."
                : "Edição: arraste componentes; botão do meio arrasta o canvas.";
    }

    private void modeButtonUpdate() {
        // The mode button is rebuilt with the screen controls after a resize; keep its label in sync now.
        for (var renderable : renderables) {
            if (renderable instanceof net.minecraft.client.gui.components.Button button
                    && (button.getMessage().getString().equals("Preview")
                    || button.getMessage().getString().equals("Edit")
                    || button.getMessage().getString().equals("P")
                    || button.getMessage().getString().equals("E"))) {
                boolean abbreviated = button.getMessage().getString().length() == 1;
                button.setMessage(Component.literal(previewMode
                        ? abbreviated ? "E" : "Edit"
                        : abbreviated ? "P" : "Preview"));
            }
        }
    }

    private void refreshInspector() {
        if (textField == null) return;
        for (AbstractWidget widget : inspectorWidgets) {
            widget.visible = !previewMode;
        }
        boolean hasSelection = selected != null;
        nameField.setEditable(!previewMode);
        textField.setValue(hasSelection && (selected.type.equals("label") || selected.type.equals("button")
                || selected.type.equals("iconbutton") || selected.type.equals("panel")
                || selected.type.equals("card") || selected.type.equals("divider")
                || selected.type.equals("toggle") || selected.type.equals("chip") || selected.type.equals("radio")
                || selected.type.equals("textfield") || selected.type.equals("searchfield")
                || selected.type.equals("itemcard") || selected.type.equals("table")
                || selected.type.equals("radial"))
                ? selected.text : "");
        colorField.setValue(!hasSelection ? "FFFFFFFF"
                : selected.usesItemStack() ? selected.itemId : String.format("%08X", selected.color));
        itemSlotBackgroundField.setValue(hasSelection && selected.type.equals("itemslot")
                && selected.itemSlotBackground != null ? selected.itemSlotBackground : "theme");
        xField.setValue(hasSelection ? Integer.toString(selected.x) : "0");
        yField.setValue(hasSelection ? Integer.toString(selected.y) : "0");
        widthField.setValue(hasSelection ? Integer.toString(selected.width) : "0");
        heightField.setValue(hasSelection ? Integer.toString(selected.height) : "0");
        gapField.setValue(hasSelection ? Integer.toString(selected.gap) : "0");
        rangeField.setValue(!hasSelection ? "0, 100, 1"
                : selected.type.equals("slider")
                    ? formatNumber(selected.minimum) + ", " + formatNumber(selected.maximum) + ", "
                        + formatNumber(selected.step)
                    : selected.type.equals("progressbar")
                    ? formatNumber(selected.minimum) + ", " + formatNumber(selected.maximum) + ", "
                        + formatNumber(selected.value)
                    : selected.supportsItemCount() ? Integer.toString(selected.itemCount)
                    : selected.supportsColumns() ? Integer.toString(selected.columns) : "0, 100, 1");
        boolean rangeEditable = hasSelection && (selected.type.equals("slider")
                || selected.type.equals("progressbar")
                || selected.supportsItemCount() || selected.supportsColumns());
        rangeField.setEditable(rangeEditable);
        boolean sliderEditable = hasSelection && selected.type.equals("slider");
        sliderValueField.setValue(sliderEditable ? formatNumber(selected.value) : "0");
        sliderUpperValueField.setValue(sliderEditable && selected.upperValue != null
                ? formatNumber(selected.upperValue) : "");
        sliderValueField.setEditable(sliderEditable && !previewMode);
        sliderUpperValueField.setEditable(sliderEditable && !previewMode);
        sliderValueField.visible = sliderEditable && !previewMode;
        sliderUpperValueField.visible = sliderEditable && !previewMode;
        boolean textEditable = hasSelection && (selected.type.equals("label") || selected.type.equals("button")
                || selected.type.equals("iconbutton") || selected.type.equals("panel")
                || selected.type.equals("card") || selected.type.equals("divider")
                || selected.type.equals("toggle") || selected.type.equals("chip") || selected.type.equals("radio")
                || selected.type.equals("textfield") || selected.type.equals("searchfield")
                || selected.type.equals("itemcard") || selected.type.equals("table")
                || selected.type.equals("radial"));
        textField.setEditable(textEditable);
        textField.visible = textEditable && !previewMode;
        boolean colorEditable = hasSelection && (selected.type.equals("label") || selected.usesItemStack());
        colorField.setEditable(colorEditable);
        colorField.visible = colorEditable && !previewMode;
        boolean itemSlotBackgroundEditable = hasSelection && selected.type.equals("itemslot");
        itemSlotBackgroundField.setEditable(itemSlotBackgroundEditable);
        itemSlotBackgroundField.visible = itemSlotBackgroundEditable && !previewMode;
        boolean gapEditable = hasSelection && selected.isContainer() && !selected.isAbsolute();
        gapField.setEditable(gapEditable);
        gapField.visible = gapEditable && !previewMode;
        boolean positionEditable = hasSelection && selected.parent != null && selected.parent.isAbsolute();
        xField.setEditable(positionEditable);
        yField.setEditable(positionEditable);
        xField.visible = positionEditable && !previewMode;
        yField.visible = positionEditable && !previewMode;
        widthField.setEditable(hasSelection);
        heightField.setEditable(hasSelection);
        widthField.visible = hasSelection && !previewMode;
        heightField.visible = hasSelection && !previewMode;
        if (horizontalAlignmentButton != null) {
            Alignment horizontal = hasSelection ? selected.horizontalAlignment : Alignment.START;
            Alignment vertical = hasSelection ? selected.verticalAlignment : Alignment.STRETCH;
            horizontalAlignmentButton.setMessage(Component.literal(alignmentLabel("H", horizontal)));
            verticalAlignmentButton.setMessage(Component.literal(alignmentLabel("V", vertical)));
            boolean selectable = hasSelection && (selected.type.equals("toggle") || selected.type.equals("radio")
                    || selected.type.equals("chip") || selected.type.equals("card"));
            enabledButton.setMessage(Component.literal(selected != null && selected.type.equals("progressbar")
                    ? "Format: " + (selected.percentage ? "Percent" : "Value")
                    : selected != null && selected.type.equals("spacer")
                    ? "Flex: " + (selected.flexible ? "Yes" : "No")
                    : selectable ? "Selected: " + (selected.selected ? "Yes" : "No")
                    : "Enabled: " + (hasSelection && selected.enabled ? "Yes" : "No")));
            boolean alignedContainer = hasSelection && selected.supportsAlignment();
            boolean gapContainer = hasSelection && selected.isContainer() && !selected.isAbsolute();
            horizontalAlignmentButton.visible = alignedContainer && !previewMode;
            verticalAlignmentButton.visible = alignedContainer && !previewMode;
            horizontalAlignmentButton.active = alignedContainer && !previewMode;
            verticalAlignmentButton.active = alignedContainer && !previewMode;
            gapField.visible = gapContainer && !previewMode;
            rangeField.visible = rangeEditable && !previewMode;
            enabledButton.visible = hasSelection && (selected.type.equals("button")
                    || selected.type.equals("iconbutton")
                    || selected.type.equals("slider") || selected.type.equals("progressbar")
                    || selected.usesItemStack()
                    || selected.type.equals("spacer") || selectable) && !previewMode;
            enabledButton.active = enabledButton.visible && !previewMode;
        }
        if (previewMode) {
            nameField.setEditable(false);
            textField.setEditable(false);
            colorField.setEditable(false);
            itemSlotBackgroundField.setEditable(false);
            xField.setEditable(false);
            yField.setEditable(false);
            widthField.setEditable(false);
            heightField.setEditable(false);
            gapField.setEditable(false);
            if (horizontalAlignmentButton != null) {
                horizontalAlignmentButton.active = false;
                verticalAlignmentButton.active = false;
                enabledButton.active = false;
            }
        }
        for (var renderable : renderables) {
            if (renderable instanceof net.minecraft.client.gui.components.Button button) {
                String label = button.getMessage().getString();
                if (label.equals("Apply") || label.equals("Remove")) {
                    button.visible = !previewMode && hasSelection;
                    button.active = !previewMode && hasSelection;
                }
            }
        }
        layoutInspectorFields();
    }

    private void layoutInspectorFields() {
        int cursorY = canvasY + 36;
        int fieldY;
        if (textField.visible) {
            fieldY = cursorY + 11;
            textField.setY(fieldY);
            cursorY += 32;
        }
        if (colorField.visible) {
            fieldY = cursorY + 11;
            colorField.setY(fieldY);
            cursorY += 32;
        }
        if (itemSlotBackgroundField.visible) {
            itemSlotBackgroundField.setY(cursorY + 11);
            cursorY += 32;
        }
        if (xField.visible || yField.visible) {
            fieldY = cursorY + 11;
            xField.setY(fieldY);
            yField.setY(fieldY);
            cursorY += 32;
        }
        if (widthField.visible || heightField.visible) {
            fieldY = cursorY + 11;
            widthField.setY(fieldY);
            heightField.setY(fieldY);
            cursorY += 32;
        }
        if (gapField.visible) {
            gapField.setY(cursorY + 11);
            cursorY += 32;
        }
        if (rangeField.visible) {
            rangeField.setY(cursorY + 11);
            cursorY += 32;
        }
        if (sliderValueField.visible) {
            sliderValueField.setY(cursorY + 11);
            sliderUpperValueField.setY(cursorY + 11);
            cursorY += 32;
        }
        int controlY = cursorY + 2;
        if (horizontalAlignmentButton.visible) {
            horizontalAlignmentButton.setY(controlY);
            verticalAlignmentButton.setY(controlY);
            controlY += 22;
        }
        if (enabledButton.visible) {
            enabledButton.setY(controlY);
            controlY += 22;
        }
        int actionY = height - 38;
        for (var renderable : renderables) {
            if (renderable instanceof net.minecraft.client.gui.components.Button button) {
                String label = button.getMessage().getString();
                if (label.equals("Apply")) button.setY(actionY);
                else if (label.equals("Remove")) button.setY(actionY);
            }
        }
    }

    private String alignmentLabel(String axis, Alignment alignment) {
        return inspectorWidth < 140 ? axis + ":" + alignment.name().charAt(0)
                : axis + ": " + alignment;
    }

    private void applyInspector() {
        if (selected == null) return;
        try {
            String nextText = textField.getValue();
            int nextColor = selected.color;
            String nextItemId = selected.itemId;
            String nextItemSlotBackground = selected.itemSlotBackground;
            if (selected.usesItemStack()) {
                nextItemId = colorField.getValue().trim();
                if (selected.type.equals("itemcard") && !PlaygroundComponentFactory.itemExists(nextItemId)) {
                    throw new IllegalArgumentException("Unknown item ID: " + nextItemId);
                }
                if (selected.type.equals("itemslot") && !nextItemId.isEmpty()
                        && !PlaygroundComponentFactory.itemExists(nextItemId)) {
                    throw new IllegalArgumentException("Unknown item ID: " + nextItemId);
                }
            } else if (selected.type.equals("label") && !colorField.getValue().isBlank()) {
                String color = colorField.getValue().replace("#", "");
                if (color.length() > 8) throw new IllegalArgumentException("Use até oito dígitos ARGB");
                nextColor = (int) Long.parseLong(color, 16);
            }
            if (selected.type.equals("itemslot")) {
                nextItemSlotBackground = itemSlotBackgroundField.getValue().trim();
                PlaygroundComponentFactory.parseItemSlotBackground(nextItemSlotBackground);
                if (nextItemSlotBackground.isBlank()) nextItemSlotBackground = "theme";
            }
            int nextX = selected.x;
            int nextY = selected.y;
            if (selected.parent != null && selected.parent.isAbsolute()) {
                nextX = parseBoundedInt(xField.getValue(), -100_000, 100_000);
                nextY = parseBoundedInt(yField.getValue(), -100_000, 100_000);
            }
            int nextWidth = parseBoundedInt(widthField.getValue(), 12, 8192);
            int nextHeight = parseBoundedInt(heightField.getValue(),
                    selected.type.equals("divider") ? 1 : 12, 8192);
            int nextGap = selected.gap;
            int nextColumns = selected.columns;
            int nextItemCount = selected.itemCount;
            double nextMinimum = selected.minimum;
            double nextMaximum = selected.maximum;
            double nextValue = selected.value;
            Double nextUpperValue = selected.upperValue;
            double nextStep = selected.step;
            if (selected.isContainer() && !selected.isAbsolute()) {
                nextGap = parseBoundedInt(gapField.getValue(), 0, 4096);
            }
            if (selected.type.equals("slider") || selected.type.equals("progressbar")) {
                String[] rangeParts = rangeField.getValue().split(",", -1);
                if (rangeParts.length != 3) throw new IllegalArgumentException(selected.type.equals("slider")
                        ? "Use min, max, step" : "Use min, max, value");
                nextMinimum = parseFiniteDouble(rangeParts[0]);
                nextMaximum = parseFiniteDouble(rangeParts[1]);
                double thirdValue = parseFiniteDouble(rangeParts[2]);
                if (selected.type.equals("slider")) nextStep = thirdValue;
                else nextValue = thirdValue;
                if (nextMaximum <= nextMinimum || (selected.type.equals("slider") && nextStep < 0)) {
                    throw new IllegalArgumentException("Min deve ser menor que max; step não pode ser negativo");
                }
            }
            if (selected.type.equals("slider")) {
                nextValue = parseFiniteDouble(sliderValueField.getValue());
                nextUpperValue = sliderUpperValueField.getValue().isBlank()
                        ? null : parseFiniteDouble(sliderUpperValueField.getValue());
                Slider slider = new Slider(nextMinimum, nextMaximum, nextValue, nextStep, ignored -> { });
                if (nextUpperValue != null) slider.setValues(nextValue, nextUpperValue);
                nextValue = slider.value();
                nextUpperValue = slider.upperValue();
            }
            if (selected.supportsColumns()) {
                nextColumns = parseBoundedInt(rangeField.getValue(), 1, 64);
            }
            if (selected.supportsItemCount()) {
                nextItemCount = parseBoundedInt(rangeField.getValue(), 1, 64);
            }
            if (selected.type.equals("label") || selected.type.equals("button")
                    || selected.type.equals("iconbutton") || selected.type.equals("panel")
                    || selected.type.equals("card") || selected.type.equals("divider")
                    || selected.type.equals("toggle") || selected.type.equals("chip") || selected.type.equals("radio")
                    || selected.type.equals("textfield") || selected.type.equals("searchfield")
                    || selected.type.equals("itemcard") || selected.type.equals("table")
                    || selected.type.equals("radial")) {
                selected.text = nextText;
            }
            selected.itemId = nextItemId;
            selected.itemSlotBackground = nextItemSlotBackground;
            selected.itemCount = nextItemCount;
            selected.columns = nextColumns;
            selected.color = nextColor;
            selected.x = nextX;
            selected.y = nextY;
            selected.width = nextWidth;
            selected.height = nextHeight;
            selected.gap = nextGap;
            if (selected.type.equals("slider")) {
                selected.minimum = nextMinimum;
                selected.maximum = nextMaximum;
                selected.step = nextStep;
                selected.value = nextValue;
                selected.upperValue = nextUpperValue;
            } else if (selected.type.equals("progressbar")) {
                selected.minimum = nextMinimum;
                selected.maximum = nextMaximum;
                selected.value = Math.max(nextMinimum, Math.min(nextMaximum, nextValue));
            }
            rebuildCanvas();
            refreshInspector();
            status = "Propriedades aplicadas.";
        } catch (IllegalArgumentException exception) {
            status = "Valor inválido: " + shortMessage(exception);
        }
    }

    private static int parseBoundedInt(String value, int minimum, int maximum) {
        int parsed = Integer.parseInt(value.trim());
        if (parsed < minimum || parsed > maximum) {
            throw new IllegalArgumentException("Valor permitido: " + minimum + " a " + maximum);
        }
        return parsed;
    }

    private static double parseFiniteDouble(String value) {
        double parsed = Double.parseDouble(value.trim());
        if (!Double.isFinite(parsed)) throw new IllegalArgumentException("Use valores numéricos finitos");
        return parsed;
    }

    private static String formatNumber(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private void cycleAlignment(boolean horizontal) {
        if (selected == null) return;
        Alignment current = horizontal ? selected.horizontalAlignment : selected.verticalAlignment;
        Alignment[] values = Alignment.values();
        Alignment next = values[(current.ordinal() + 1) % values.length];
        if (horizontal) selected.horizontalAlignment = next;
        else selected.verticalAlignment = next;
        rebuildCanvas();
        refreshInspector();
    }

    private void toggleEnabled() {
        if (selected == null) return;
        if (selected.type.equals("progressbar")) {
            selected.percentage = !selected.percentage;
        } else if (selected.type.equals("spacer")) {
            selected.flexible = !selected.flexible;
        } else if (selected.type.equals("toggle") || selected.type.equals("chip")
                || selected.type.equals("radio") || selected.type.equals("card")) {
            selected.selected = !selected.selected;
        } else {
            selected.enabled = !selected.enabled;
        }
        rebuildCanvas();
        refreshInspector();
    }

    private void removeSelected() {
        if (selected == null || selected == document.root || selected.parent == null) return;
        selected.parent.removeChild(selected);
        selected = null;
        rebuildCanvas();
        refreshInspector();
        status = "Componente removido.";
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
