package com.radaeli.ruilib.client.playground;

import com.google.gson.GsonBuilder;
import com.radaeli.ruilib.ui.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Opt-in development regression runner. Uses the real client, fonts, widgets and renderer. */
public final class UiValidation extends UiScreen {
    private static final String[] TYPES = {"label", "button", "iconbutton", "divider", "panel", "card",
            "toggle", "chip", "textfield", "searchfield", "slider", "progressbar", "radial", "itemcard",
            "itemslot", "spacer", "row", "column", "stack", "tabs", "scroll", "clip", "grid", "itemgrid", "table", "absolute"};
    private static boolean started;
    private final List<String> checks = new ArrayList<>();
    private final List<String> captures = new ArrayList<>();
    private final Path output = Minecraft.getInstance().gameDirectory.toPath().resolve("ui-validation");
    private int frame;
    private boolean tested;
    private Button motionButton;

    private UiValidation() { super(Component.literal("R-UI validation"), new AbsoluteLayout()); }

    public static void clientTick() {
        if (!Boolean.getBoolean("r_ui_lib.validate") || started) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen == null || mc.getOverlay() != null || mc.font == null) return;
        started = true;
        mc.options.guiScale().set(2);
        GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), 1280, 960);
        mc.resizeDisplay();
        mc.setScreen(new UiValidation());
    }

    private void check(boolean success, String name) {
        if (!success) throw new AssertionError(name);
        checks.add(name);
    }

    private void click(UiComponent component) {
        Bounds b = component.bounds();
        mouseClicked(b.centerX(), b.centerY(), 0);
        mouseReleased(b.centerX(), b.centerY(), 0);
    }

    private void inputChecks() {
        AbsoluteLayout root = (AbsoluteLayout) root();
        int[] actions = {0};
        Button button = root.addAt(new Button(Component.literal("Action"), () -> actions[0]++), new Bounds(10, 10, 100, 24));
        TextField text = root.addAt(new TextField(), new Bounds(10, 45, 180, 24));
        SearchField search = root.addAt(new SearchField(), new Bounds(10, 80, 180, 24));
        Slider slider = root.addAt(new Slider(0, 100, 50, 1, value -> { }), new Bounds(10, 115, 220, 24));
        Toggle toggle = root.addAt(new Toggle(Component.literal("Toggle"), value -> { }), new Bounds(10, 150, 100, 24));
        Chip chip = root.addAt(new Chip(Component.literal("Chip"), value -> { }), new Bounds(120, 150, 70, 24));
        IconButton icon = root.addAt(new IconButton(Component.literal("Icon"), Component.literal("+"), () -> actions[0]++),
                new Bounds(200, 150, 24, 24));
        Select<Integer> select = root.addAt(new Select<>(List.of(new Select.Option<>(0, Component.literal("Zero")),
                new Select.Option<>(1, Component.literal("One"))), 0, value -> { }), new Bounds(10, 185, 180, 24));
        ScrollView scroll = root.addAt(new ScrollView(4), new Bounds(260, 10, 180, 80));
        for (int i = 0; i < 12; i++) scroll.add(new Label(Component.literal("Row " + i)));
        SimpleContainer inventory = new SimpleContainer(9);
        inventory.setItem(0, new ItemStack(Items.DIAMOND, 32));
        InventoryGrid picker = root.addAt(new InventoryGrid(inventory).setInteractionMode(InventoryGrid.InteractionMode.PICKER),
                new Bounds(260, 120, 220, 24));
        ItemSlot slot = root.addAt(new ItemSlot(), new Bounds(260, 160, 24, 24));
        int[] droppedCount = {0};
        slot.setOnDrop(stack -> droppedCount[0] = stack.getCount());
        Tabs tabs = root.addAt(new Tabs().addTab(Component.literal("First"), new Label(Component.literal("One")))
                .addTab(Component.literal("Second"), new Label(Component.literal("Two"))), new Bounds(260, 210, 180, 64));
        Table<String> table = root.addAt(new Table<String>()
                .addColumn(TableColumn.preferred(Component.literal("Name"), Alignment.START, Component::literal))
                .setRows(List.of("One", "Two", "Three", "Four", "Five", "Six")), new Bounds(260, 290, 180, 70));
        int[] radialActions = {0};
        RadialMenu radial = root.addAt(new RadialMenu()
                .addSegment(new RadialSegment(Component.literal("First"), true, () -> radialActions[0]++))
                .addSegment(new RadialSegment(Component.literal("Second"), true, () -> radialActions[0]++)),
                new Bounds(480, 10, 144, 144));
        layoutRoot(theme());

        click(button); check(actions[0] == 1, "button mouse callback");
        keyPressed(GLFW.GLFW_KEY_ENTER, 0, 0); check(actions[0] == 2, "button keyboard callback");
        button.setEnabled(false); click(button); check(actions[0] == 2, "disabled button rejects mouse");
        button.setEnabled(true);
        click(text); charTyped('A', 0); charTyped('\u00e7', 0);
        check(text.value().equals("A\u00e7"), "text input unicode");
        keyPressed(GLFW.GLFW_KEY_BACKSPACE, 0, 0); check(text.value().equals("A"), "text backspace");
        text.setValue("abcdef");
        mouseClicked(14, 55, 0); mouseDragged(14 + font.width("abc"), 55, 0, font.width("abc"), 0);
        mouseReleased(14 + font.width("abc"), 55, 0); charTyped('Z', 0);
        check(text.value().equals("Zdef"), "text mouse drag selection replaces prefix");
        text.setValue("abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz");
        mouseClicked(184, 55, 0);
        for (int i = 0; i < 80; i++) mouseDragged(-20, 55, 0, -10, 0);
        mouseReleased(-20, 55, 0); charTyped('Q', 0);
        check(text.value().startsWith("Q") && text.value().length() < 10, "text drag autoscroll across long value");
        UiTokens tokens = theme().tokens();
        UiTheme scaledTheme = new UiTheme(theme().palette(), new UiTokens(tokens.panelPadding(), tokens.cardPadding(),
                tokens.smallGap(), tokens.mediumGap(), tokens.largeGap(), tokens.buttonPadding(), tokens.buttonMinimumWidth(),
                tokens.buttonMinimumHeight(), new UiTypography(1, 1.5f, 1.5f, 1.5f), tokens.radii(), tokens.motion()));
        text.layout(font, scaledTheme, text.bounds()); text.setValue("abcdef");
        mouseClicked(14, 55, 0); mouseDragged(14 + font.width("abc") * 1.5, 55, 0, font.width("abc") * 1.5, 0);
        mouseReleased(14 + font.width("abc") * 1.5, 55, 0); charTyped('Z', 0);
        check(text.value().equals("Zdef"), "scaled text selection uses logical coordinates");
        text.layout(font, theme(), text.bounds());
        // Native EditBox uses Screen.hasControlDown(), so clipboard shortcuts are covered separately by the manual matrix.
        text.setMaxLength(3); text.setValue(""); charTyped('1', 0); charTyped('2', 0); charTyped('3', 0); charTyped('4', 0);
        check(text.value().equals("123"), "text max length");
        text.setEnabled(false); charTyped('5', 0); check(text.value().equals("123"), "disabled text rejects typing");
        text.setEnabled(true); click(text); keyPressed(GLFW.GLFW_KEY_TAB, 0, 0);
        check(search.isFocused(), "tab advances focus");
        charTyped('x', 0); check(search.value().equals("x"), "search text input");
        keyPressed(GLFW.GLFW_KEY_TAB, 0, GLFW.GLFW_MOD_SHIFT); check(text.isFocused(), "shift tab reverses focus");
        click(slider); double before = slider.value(); keyPressed(GLFW.GLFW_KEY_RIGHT, 0, 0);
        check(slider.value() > before, "slider keyboard step");
        mouseClicked(17, 127, 0); mouseDragged(160, 127, 0, 143, 0); mouseReleased(160, 127, 0);
        check(slider.value() > 50, "slider drag capture");
        slider.setValues(20, 80); slider.setValue(100);
        check(slider.value() < slider.upperValue(), "range handles remain ordered");
        click(toggle); check(toggle.isSelected(), "toggle mouse");
        keyPressed(GLFW.GLFW_KEY_SPACE, 0, 0); check(!toggle.isSelected(), "toggle keyboard");
        click(chip); check(chip.isSelected(), "chip mouse");
        keyPressed(GLFW.GLFW_KEY_SPACE, 0, 0); check(!chip.isSelected(), "chip keyboard");
        click(icon); check(actions[0] == 3, "icon mouse callback");
        keyPressed(GLFW.GLFW_KEY_ENTER, 0, 0); check(actions[0] == 4, "icon keyboard callback");
        click(select); check(select.isOpen(), "select opens");
        keyPressed(GLFW.GLFW_KEY_DOWN, 0, 0); keyPressed(GLFW.GLFW_KEY_ENTER, 0, 0);
        check(select.value() == 1 && !select.isOpen(), "select keyboard commits");
        click(select); keyPressed(GLFW.GLFW_KEY_ESCAPE, 0, 0); check(!select.isOpen(), "select escape closes");
        select.setValue(0); click(select);
        // UiScreen resolves popup placement when it next receives an event.
        keyPressed(GLFW.GLFW_KEY_DOWN, 0, 0);
        mouseClicked(30, 242, 0); mouseReleased(30, 242, 0);
        check(select.value() == 1 && !select.isOpen(), "select popup mouse commit");
        click(select); mouseClicked(240, 420, 0); check(!select.isOpen(), "select outside dismissal");
        check(scroll.maxScrollOffset() > 0, "scroll overflow measured");
        // Dispatch via Minecraft's actual version-specific GuiEventListener signature.
        com.radaeli.ruilib.platform.UiPlatform.scroll(this, 280, 40, -1);
        check(scroll.scrollOffset() > 0, "native wheel reaches toolkit");
        mouseClicked(272, 132, 0); mouseDragged(272, 172, 0, 0, 40); mouseReleased(272, 172, 0);
        check(slot.itemStack().is(Items.DIAMOND) && slot.itemStack().getCount() == 1
                && droppedCount[0] == 32 && inventory.getItem(0).getCount() == 32, "inventory ghost drag preserves original");
        click(slot); keyPressed(GLFW.GLFW_KEY_DELETE, 0, 0); check(slot.itemStack().isEmpty(), "ghost slot delete");
        mouseClicked(272, 132, 1); mouseReleased(272, 132, 1); click(slot);
        check(slot.itemStack().getCount() == 1 && droppedCount[0] == 1 && inventory.getItem(0).getCount() == 32, "inventory right click picks one");
        mouseClicked(270, 218, 0); mouseReleased(270, 218, 0); keyPressed(GLFW.GLFW_KEY_RIGHT, 0, 0);
        check(tabs.selectedIndex() == 1, "tabs keyboard navigation");
        mouseClicked(270, 322, 0); mouseReleased(270, 322, 0);
        check(table.selectedIndex() >= 0, "table row click");
        com.radaeli.ruilib.platform.UiPlatform.scroll(this, 280, 320, -1);
        check(table.scrollOffset() > 0, "table native scroll");
        radial.open(); mouseClicked(552, 82, 0); mouseDragged(580, 70, 0, 28, -12); mouseReleased(580, 70, 0);
        check(radialActions[0] == 1, "radial drag selects segment");
        Modal modal = new Modal(Component.literal("Modal"), new TextField());
        showModal(modal); check(modal() == modal, "modal opens");
        keyPressed(GLFW.GLFW_KEY_ESCAPE, 0, 0); check(modal() == null, "modal escape closes");
        toastHost().info(Component.literal("Validation toast")); check(toastHost().size() == 1, "toast lifecycle starts");
        if (Boolean.getBoolean("r_ui_lib.validateJei")) {
            closeModal();
            UiJeiValidation.verify(this, slot);
            checks.add("JEI gui properties and ghost targets");
        }
    }

    private void colorControlChecks() {
        AbsoluteLayout root=(AbsoluteLayout)root();
        for(var child:List.copyOf(root.children()))root.remove(child);
        int[] committed={-1};int[] cancelled={0};
        ColorPicker picker=new ColorPicker(0x123456,value->committed[0]=value,()->cancelled[0]++);
        showModal(new Modal(Component.literal("Color"),picker,260,270));
        List<TextField> fields=new ArrayList<>();List<Button> buttons=new ArrayList<>();List<Slider> sliders=new ArrayList<>();
        picker.visitComponents(c->{if(c instanceof TextField f)fields.add(f);if(c instanceof Button b)buttons.add(b);if(c instanceof Slider v)sliders.add(v);});
        fields.get(0).setValue("#ABCDEF");check(picker.value()==0xabcdef && committed[0]==-1,"color preview does not commit");
        fields.get(0).setValue("#bad");check(!buttons.get(0).isEnabled(),"invalid hex prevents apply");
        fields.get(0).setValue("#ABCDEF");click(sliders.get(0));keyPressed(GLFW.GLFW_KEY_LEFT,0,0);check(picker.value()!=0xabcdef,"color slider keyboard edits preview");
        click(buttons.get(0));check(committed[0]==picker.value(),"color apply commits preview");
        committed[0]=-1;click(buttons.get(1));check(cancelled[0]==1 && committed[0]==-1,"color cancel leaves committed value untouched");
        closeModal();
        HorizontalScrollView scroll=root.addAt(new HorizontalScrollView(5),new Bounds(10,10,160,80));
        for(int i=0;i<5;i++){Button b=scroll.add(new Button(Component.literal("Item "+i),()->{}));b.setPreferredSize(100,30);}
        root.layout(font,theme(),new Bounds(0,0,width,height));
        check(scroll.maxScrollOffset()>0,"horizontal overflow measured");
        scroll.mouseScrolled(40,40,0,-1);check(scroll.scrollOffset()>0,"horizontal wheel scrolls");
        scroll.setFocused(true);int before=scroll.scrollOffset();scroll.keyPressed(GLFW.GLFW_KEY_RIGHT,0,0);check(scroll.scrollOffset()>before,"horizontal keyboard scrolls");
        scroll.mouseClicked(140,86,0);scroll.mouseDragged(160,86,0,20,0);scroll.mouseReleased(160,86,0);check(scroll.scrollOffset()>0,"horizontal thumb drag");
        var customTooltip=new ColorSwatch(0x123456,false,()->{});var tooltip=Tooltip.of(Component.literal("#123456 · 25%"));customTooltip.tooltip(tooltip);
        check(customTooltip.tooltip()==tooltip,"color swatch preserves custom percentage tooltip");
        int[] parentActions={0},childActions={0};
        Column cardContent=new Column(4);
        cardContent.add(new Label(Component.literal("Select background")));
        Button child=cardContent.add(new Button(Component.literal("Child"),()->childActions[0]++));
        SelectableCard card=root.addAt(new SelectableCard(cardContent,false,()->parentActions[0]++),new Bounds(300,120,200,100));
        SegmentedControl<Integer> segments=root.addAt(new SegmentedControl<>(List.of(
            new SegmentedControl.Option<>(0,Component.literal("Free")),
            new SegmentedControl.Option<>(1,Component.literal("Disabled"),false),
            new SegmentedControl.Option<>(2,Component.literal("Color"))),0,v->childActions[0]++),new Bounds(300,240,250,26));
        int[] radioChanges={0};
        RadioGroup<Integer> radios=root.addAt(new RadioGroup<>(List.of(
            new RadioGroup.Option<>(0,Component.literal("Free")),
            new RadioGroup.Option<>(1,Component.literal("Disabled"),false),
            new RadioGroup.Option<>(2,Component.literal("Color"))),null,v->radioChanges[0]++),new Bounds(300,280,200,54));
        Radio radio=root.addAt(new Radio(Component.literal("Radio"),false,()->radioChanges[0]++),new Bounds(520,280,100,16));
        int[] stepperValue={-1};
        Stepper<Integer> stepper=root.addAt(Stepper.ofRange(Component.literal("Near"),0,8,1,7,v->Component.literal(v+" m"),
            v->stepperValue[0]=v),new Bounds(300,340,160,12));
        Stepper<String> sides=root.addAt(Stepper.ofOptions(Component.literal("Looks at"),List.of("Front","Rear"),null,
            Component::literal,Component.literal("Not set"),true,v->{}),new Bounds(300,356,160,12));
        stepper.tooltip(Component.literal("Range help"));
        layoutRoot(theme());
        var hit=card.hitTest(child.bounds().centerX(),child.bounds().centerY());
        hit.mouseClicked(child.bounds().centerX(),child.bounds().centerY(),0);hit.mouseReleased(child.bounds().centerX(),child.bounds().centerY(),0);
        check(childActions[0]==1&&parentActions[0]==0,"card child consumes input without selecting parent");
        check(card.hitTest(305,125)==card,"passive card background selects frame");
        card.setFocused(true);card.keyPressed(GLFW.GLFW_KEY_ENTER,0,0);
        check(parentActions[0]==1,"card keyboard activation");
        segments.setFocused(true);segments.keyPressed(GLFW.GLFW_KEY_RIGHT,0,0);
        check(segments.value()==2,"segmented arrows skip disabled option");
        segments.mouseClicked(420,250,0);segments.mouseReleased(420,250,0);
        check(segments.value()==2,"segmented disabled option rejects click");
        segments.keyPressed(GLFW.GLFW_KEY_HOME,0,0);check(segments.value()==0,"segmented home selects first enabled option");
        check(radios.value()==null,"radio group may start empty");
        radios.mouseClicked(310,280+27,0);radios.mouseReleased(310,280+27,0);
        check(radios.value()==null&&radioChanges[0]==0,"radio group disabled option rejects click");
        radios.mouseClicked(310,280+45,0);radios.mouseReleased(310,280+45,0);
        check(radios.value()==2&&radioChanges[0]==1,"radio group mouse selects on release");
        radios.keyPressed(GLFW.GLFW_KEY_UP,0,0);check(radios.value()==0,"radio group arrows skip disabled option");
        radios.keyPressed(GLFW.GLFW_KEY_END,0,0);check(radios.value()==2,"radio group end selects last enabled option");
        radios.setValue(0);check(radioChanges[0]==3,"radio group setValue skips callback");
        radio.mouseClicked(525,285,0);radio.mouseReleased(525,285,0);
        check(radio.isSelected()&&radioChanges[0]==4,"radio mouse selects");
        radio.mouseClicked(525,285,0);radio.mouseReleased(525,285,0);
        check(radio.isSelected()&&radioChanges[0]==4,"radio click never clears selection");
        stepper.mouseScrolled(380,346,0,1);check(stepperValue[0]==8,"stepper wheel steps up");
        stepper.mouseScrolled(380,346,0,1);check(stepper.value()==8&&stepperValue[0]==8,"stepper holds at its maximum");
        check(stepper.tooltipAt(301,346)!=null&&stepper.tooltipAt(455,346)==null,"stepper tooltip stays on the label");
        sides.mouseScrolled(380,362,0,-1);check("Rear".equals(sides.value()),"option stepper starts backwards at the last option");
        sides.mouseScrolled(380,362,0,1);check("Front".equals(sides.value()),"option stepper wraps");
                focusComponent(segments);check(focusedComponent()==segments&&segments.isFocused(),"screen exposes focus for tree replacement");
        focusComponent(card);check(card.isFocused()&&!segments.isFocused(),"screen transfers visible focus to replacement");
        for(var layout:UiPlaygroundScreen.builtInLayouts()){
            try(var stream=UiValidation.class.getResourceAsStream("/assets/r_ui_lib/playground/layouts/"+layout.id()+".json")){
                var document=new GsonBuilder().create().fromJson(new java.io.InputStreamReader(stream,java.nio.charset.StandardCharsets.UTF_8),PlaygroundDocument.class);document.normalize();
                PlaygroundComponentFactory.build(document.root,PlaygroundComponentFactory.newComponentMap(),n->{},(n,v)->{});check(true,"built-in layout "+layout.id());
            }catch(Exception e){throw new IllegalStateException(e);}
        }
    }

    private void newControlsCapture(GuiGraphics graphics,int state) throws Exception {
        var root=new AbsoluteLayout();
        var swatch=root.addAt(new ColorSwatch(0x4e91bd,true,()->{}),new Bounds(20,30,32,32));
        root.addAt(new ItemPreview(new ItemStack(Items.OAK_LOG),64),new Bounds(70,20,64,64));
        root.addAt(new ColorPicker(0x4e91bd,c->{},()->{}),new Bounds(20,110,220,220));
        var scroll=root.addAt(new HorizontalScrollView(6),new Bounds(280,40,240,100));
        for(int i=0;i<6;i++) {var panel=scroll.add(new Panel(new ItemPreview(new ItemStack(Items.BRICKS),48)).setBackground(0xff604040+i*0x10101));panel.setPreferredSize(100,80);}
        Column composite=new Column(5);composite.add(new ItemPreview(new ItemStack(Items.STONE),40));
        composite.add(new IconButton(Component.literal("Keep block"),state==0?IconButton.Symbol.LOCK_OPEN:IconButton.Symbol.LOCK_CLOSED,()->{}));
        var card=root.addAt(new SelectableCard(composite,state>0,()->{}),new Bounds(290,175,145,115));card.setFocused(state==2);
        root.addAt(new SegmentedControl<>(List.of(new SegmentedControl.Option<>(0,Component.literal("Free")),new SegmentedControl.Option<>(1,Component.literal("Block")),new SegmentedControl.Option<>(2,Component.literal("Color"))),state,v->{}),new Bounds(280,310,260,26));
        root.layout(font,UiTheme.DARK,new Bounds(0,0,width,height));if(state>0)scroll.setScrollOffset(120);
        graphics.fill(0,0,width,height,0xff101319);root.render(new UiContext(graphics,font,UiTheme.DARK,state==2?95:-1,state==2?45:-1,width,height));
        if(state==2){
            var dialog=new Modal(Component.literal("Dialog above item previews"),new ColorPicker(0x4e91bd,c->{},()->{}),260,270);
            dialog.layoutInScreen(font,UiTheme.DARK,width,height);
            dialog.render(new UiContext(graphics,font,UiTheme.DARK,-1,-1,width,height,0));
            dialog.render(new UiContext(graphics,font,UiTheme.DARK,-1,-1,width,height,200_000_000));
        }
        capture(graphics,"palette-controls-"+state);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        try {
            if (!tested) {
                Files.createDirectories(output);
                Files.deleteIfExists(output.resolve("failure.txt"));
                Files.deleteIfExists(output.resolve("results.json"));
                inputChecks(); colorControlChecks(); tested = true;
            }
            if (frame < 18) {
                int page = frame % 3;
                boolean dark = frame % 6 < 3;
                int state = frame / 6;
                UiTheme theme = dark ? UiTheme.DARK : UiTheme.LIGHT;
                graphics.fill(0, 0, width, height, theme.palette().screen() | 0xFF000000);
                for (int i = page * 9; i < Math.min(TYPES.length, page * 9 + 9); i++) {
                    PlaygroundNode node = PlaygroundNode.create(TYPES[i], 0, 0);
                    if (node.isContainer()) node.children.add(PlaygroundNode.create("button", 2, 2));
                    UiComponent component = PlaygroundComponentFactory.build(node, PlaygroundComponentFactory.newComponentMap(), n -> { }, (n, selected) -> { });
                    int cell = i - page * 9;
                    Bounds bounds = new Bounds(12 + cell % 3 * 208, 30 + cell / 3 * 144, Math.min(198, node.width), Math.min(126, node.height));
                    component.layout(font, theme, bounds);
                    graphics.drawString(font, TYPES[i], bounds.x(), bounds.y() - 14, theme.palette().text(), false);
                    component.render(new UiContext(graphics, font, theme, -100, -100, width, height, 0));
                    if (state > 0) {
                        component.setFocused(true);
                        component.mouseClicked(bounds.centerX(), bounds.centerY(), 0);
                        component.mouseReleased(bounds.centerX(), bounds.centerY(), 0);
                        if (component instanceof TextField field) { field.setValue("Sample 123"); field.setFocused(false); }
                        if (state == 2) component.setEnabled(false);
                        component.render(new UiContext(graphics, font, theme, bounds.centerX(), bounds.centerY(), width, height, 0));
                        component.render(new UiContext(graphics, font, theme, bounds.centerX(), bounds.centerY(), width, height, 70_000_000));
                    }
                    checks.add("render " + TYPES[i] + " " + (dark ? "dark" : "light") + " state " + state);
                }
                capture(graphics, (dark ? "dark" : "light") + "-atlas-" + page + "-state-" + state);
            } else if (frame < 24) {
                int step = frame - 18;
                graphics.fill(0, 0, width, height, UiTheme.DARK.palette().screen() | 0xFF000000);
                if (motionButton == null) {
                    motionButton = new Button(Component.literal("Hover transition"), () -> { });
                    motionButton.layout(font, UiTheme.DARK, new Bounds(40, 40, 200, 32));
                    motionButton.render(new UiContext(graphics, font, UiTheme.DARK, -1, -1, width, height, 0));
                }
                long time = Math.max(0, step - 1) * 35_000_000L;
                motionButton.render(new UiContext(graphics, font, UiTheme.DARK, step == 0 ? -1 : 60, 50, width, height, time));
                capture(graphics, "motion-" + step);
            } else if (frame < 29) {
                int step = frame - 24;
                graphics.fill(0, 0, width, height, UiTheme.DARK.palette().screen() | 0xFF000000);
                UiOverlayValidation.render(graphics, font, width, height, step);
                capture(graphics, "overlays-" + step);
            } else if (frame < 32) {
                graphics.fill(0, 0, width, height, UiTheme.DARK.palette().screen() | 0xFF000000);
                UiOverlayValidation.renderCursor(graphics, font, width, height, frame - 29);
                capture(graphics, "cursor-" + (frame - 29));
            } else if(frame < 35) {
                newControlsCapture(graphics,frame-32);
            } else {
                Files.writeString(output.resolve("results.json"), new GsonBuilder().setPrettyPrinting().create()
                        .toJson(java.util.Map.of("passed", true, "checks", checks, "captures", captures)));
                Minecraft.getInstance().stop();
            }
            frame++;
        } catch (Exception | AssertionError error) {
            try { Files.createDirectories(output); Files.writeString(output.resolve("failure.txt"), error.toString()); } catch (Exception ignored) { }
            throw new IllegalStateException("R-UI runtime validation failed", error);
        }
    }

    private void capture(GuiGraphics graphics, String name) throws Exception {
        graphics.flush();
        try (var image = Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) {
            image.writeToFile(output.resolve(name + ".png"));
        }
        captures.add(name);
    }
}
