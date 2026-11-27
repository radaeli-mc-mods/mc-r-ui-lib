package com.radaeli.ruilib.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.IntConsumer;

/** Single-selection page container with a compact, keyboard-navigable tab strip. */
public final class Tabs extends UiContainer {
    public enum Placement { TOP, LEFT, RIGHT }
    private record Tab(Component title, UiIcon icon, UiComponent content) { }
    private Placement placement=Placement.TOP;
    private Bounds[] sideBounds=new Bounds[0];
    public Tabs setPlacement(Placement placement){this.placement=Objects.requireNonNull(placement);if(lastFont!=null)layoutChildren(lastFont,lastTheme);return this;}
    public Tabs addTab(Component title,UiIcon icon,UiComponent content){addTab(title,content);tabs.set(tabs.size()-1,new Tab(title,icon,content));return this;}

    private final List<Tab> tabs = new ArrayList<>();
    private int selectedIndex;
    private int stripHeight;
    private int[] tabWidths = new int[0];
    private Alignment tabAlignment = Alignment.START;
    private Font lastFont;
    private UiTheme lastTheme;
    private IntConsumer onSelectionChanged = index -> { };

    public Tabs addTab(Component title, UiComponent content) {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(content, "content");
        super.add(content);
        tabs.add(new Tab(title,null,content));
        return this;
    }

    public int selectedIndex() {
        return selectedIndex;
    }

    /** Sets the horizontal alignment of the tab strip without changing the content bounds. */
    public Tabs setTabAlignment(Alignment alignment) {
        tabAlignment = Objects.requireNonNull(alignment, "alignment");
        return this;
    }

    public void setOnSelectionChanged(IntConsumer listener) {
        onSelectionChanged = Objects.requireNonNull(listener, "listener");
    }

    public void select(int index) {
        if (index < 0 || index >= tabs.size()) throw new IndexOutOfBoundsException("tab index: " + index);
        if (selectedIndex == index) return;
        selectedIndex = index;
        if (lastFont != null) layoutChildren(lastFont, lastTheme);
        onSelectionChanged.accept(index);
    }

    @Override
    protected UiSize measureIntrinsic(Font font, UiTheme theme, int maxWidth, int maxHeight) {
        if(placement!=Placement.TOP){UiSize child=tabs.isEmpty()?UiSize.ZERO:tabs.get(selectedIndex).content().measure(font,theme,Math.max(0,maxWidth-28),maxHeight);return new UiSize(child.width()+28,Math.max(child.height(),tabs.size()*26)).constrain(maxWidth,maxHeight);}
        int stripWidth = 0;
        int stripHeight = Math.max(UiText.height(font, theme) + 8, 16);
        for (Tab tab : tabs) stripWidth += UiText.width(font, tab.title(), theme) + 16;
        UiSize contentSize = tabs.isEmpty() ? UiSize.ZERO
                : tabs.get(selectedIndex).content().measure(font, theme, maxWidth, Math.max(0, maxHeight - stripHeight));
        return new UiSize(Math.max(stripWidth, contentSize.width()), stripHeight + contentSize.height())
                .constrain(maxWidth, maxHeight);
    }

    @Override
    protected void layoutChildren(Font font, UiTheme theme) {
        lastFont = font;
        lastTheme = theme;
        if (tabs.isEmpty()) return;
        if(placement!=Placement.TOP){
            int strip=Math.min(28,bounds().width());
            int contentX=bounds().x()+(placement==Placement.LEFT?strip:0);
            Bounds content=new Bounds(contentX,bounds().y(),Math.max(0,bounds().width()-strip),bounds().height());
            sideBounds=new Bounds[tabs.size()];
            for(int i=0;i<tabs.size();i++){
                int w=Math.min(strip,i==selectedIndex?28:24),y=bounds().y()+i*26;
                int x=placement==Placement.RIGHT?bounds().right()-strip:bounds().x()+strip-w;
                sideBounds[i]=new Bounds(x,y,w,Math.min(24,Math.max(0,bounds().bottom()-y)));
                tabs.get(i).content().layout(font,theme,i==selectedIndex?content:Bounds.EMPTY);
            }return;
        }
        stripHeight = Math.min(bounds().height(), Math.max(UiText.height(font, theme) + 8, 16));
        tabWidths = new int[tabs.size()];
        for (int i = 0; i < tabs.size(); i++) tabWidths[i] = UiText.width(font, tabs.get(i).title(), theme) + 16;
        UiComponent active = tabs.get(selectedIndex).content();
        active.layout(font, theme, new Bounds(bounds().x(), bounds().y() + stripHeight,
                bounds().width(), Math.max(0, bounds().height() - stripHeight)));
        for (int i = 0; i < tabs.size(); i++) {
            if (i != selectedIndex) tabs.get(i).content().layout(font, theme, Bounds.EMPTY);
        }
    }

    @Override
    public void render(UiContext context) {
        if (tabs.isEmpty()) return;
        if(placement!=Placement.TOP){
            tabs.get(selectedIndex).content().render(context);
            for(int i=0;i<tabs.size();i++){
                Bounds b=sideBounds[i];if(b.height()==0)continue;boolean selected=i==selectedIndex;
                UiPaint.tab(context,b,selected?1:0,selected&&isFocused()?1:0);
                if(selected&&context.theme().skin().pixelated()){
                    int edge=placement==Placement.RIGHT?b.x():b.right()-2;
                    UiPaint.rect(context,new Bounds(edge,b.y()+2,2,Math.max(0,b.height()-4)),context.theme().palette().buttonPressed());
                }
                UiIcon icon=tabs.get(i).icon()==null?UiIcon.glyph(Component.literal(tabs.get(i).title().getString().isEmpty()?"?":tabs.get(i).title().getString().substring(0,1))):tabs.get(i).icon();
                icon.render(context,b.centerX()-icon.width(context.font(),context.theme())/2,b.centerY()-icon.height(context.font(),context.theme())/2,
                    context.theme().skin().buttonText(context.theme(),isEnabled(),selected));
            }return;
        }
        int x = bounds().x() + tabStripOffset();
        for (int i = 0; i < tabs.size(); i++) {
            Component title = tabs.get(i).title();
            int width = Math.min(bounds().right() - x, tabWidths[i]);
            if (width <= 0) break;
            Bounds tabBounds = new Bounds(x, bounds().y(), width, stripHeight);
            boolean selected = i == selectedIndex;
            double selectedProgress = animate(context, "selected-tab-" + i, selected ? 1 : 0);
            double focusProgress = animate(context, "focus-tab-" + i,
                    selected && isFocused() ? 1 : 0);
            UiPaint.tab(context, tabBounds, selectedProgress, focusProgress, i == 0,
                    i == tabs.size() - 1 || x + width >= bounds().right());
            UiPaint.textCentered(context, title, tabBounds.centerX(),
                    tabBounds.centerY() - UiText.height(context) / 2,
                    context.theme().skin().pixelated()?context.theme().skin().buttonText(context.theme(),isEnabled(),selected):UiPaint.mixColor(context.theme().palette().mutedText(), context.theme().palette().text(), selectedProgress));
            x += width;
        }
        tabs.get(selectedIndex).content().render(context);
    }

    @Override
    public UiComponent hitTest(double mouseX, double mouseY) {
        if (!isEnabled() || !bounds().contains(mouseX, mouseY) || tabs.isEmpty()) return null;
        if(placement!=Placement.TOP){for(Bounds b:sideBounds)if(b.contains(mouseX,mouseY))return this;return tabs.get(selectedIndex).content().hitTest(mouseX,mouseY);}
        if (mouseY < bounds().y() + stripHeight) return this;
        return tabs.get(selectedIndex).content().hitTest(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if(!isEnabled()||button!=0||tabs.isEmpty())return false;
        if(placement!=Placement.TOP){for(int i=0;i<sideBounds.length;i++)if(sideBounds[i].contains(mouseX,mouseY)){select(i);setFocused(true);return true;}return false;}
        if(mouseY>=bounds().y()+stripHeight)return false;
        int x = bounds().x() + tabStripOffset();
        for (int i = 0; i < tabs.size(); i++) {
            int width = tabWidths[i];
            if (mouseX >= x && mouseX < x + width) {
                select(i);
                setFocused(true);
                return true;
            }
            x += width;
        }
        return true;
    }

    private int tabStripOffset() {
        if (tabAlignment != Alignment.CENTER) return 0;
        int stripWidth = 0;
        for (int width : tabWidths) stripWidth += width;
        return Math.max(0, (bounds().width() - stripWidth) / 2);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!isEnabled() || !isFocused() || tabs.size() < 2) return false;
        if (keyCode == (placement==Placement.TOP?GLFW.GLFW_KEY_LEFT:GLFW.GLFW_KEY_UP)) {
            select((selectedIndex + tabs.size() - 1) % tabs.size());
            return true;
        }
        if (keyCode == (placement==Placement.TOP?GLFW.GLFW_KEY_RIGHT:GLFW.GLFW_KEY_DOWN)) {
            select((selectedIndex + 1) % tabs.size());
            return true;
        }
        return false;
    }

    @Override
    public void visitFocusable(java.util.function.Consumer<UiComponent> visitor) {
        if (!isEnabled()) return;
        if (tabs.size() > 1) visitor.accept(this);
        if (!tabs.isEmpty()) tabs.get(selectedIndex).content().visitFocusable(visitor);
    }

    @Override public Tooltip tooltipAt(double x,double y){
        if(placement!=Placement.TOP){for(int i=0;i<sideBounds.length;i++)if(sideBounds[i].contains(x,y))return Tooltip.of(tabs.get(i).title());}
        return super.tooltipAt(x,y);
    }
    @Override public Tooltip tooltip(){return isFocused()&&!tabs.isEmpty()?Tooltip.of(tabs.get(selectedIndex).title()):super.tooltip();}
    @Override
    public boolean isFocusable() {
        return tabs.size() > 1;
    }
}
