package com.radaeli.ruilib.ui;
import com.radaeli.ruilib.ui.*;
import net.minecraft.network.chat.Component;

public final class WorkshopExample {
    public static UiScreen screen() {
        Column content = new Column(4);
        content.add(new Panel(new Label(Component.literal("Workshop")))
                .setSurface(SurfaceVariant.TITLE_PLATE));
        content.add(new Label(Component.literal("A long title stays inside its row"))
                .setOverflow(UiText.Overflow.ELLIPSIS, 1));
        IconButton play = new IconButton(Component.literal("Play"), UiIcons.PLAY, () -> { });
        play.setSelected(true);
        content.add(play);
        Tabs tabs = new Tabs().setPlacement(Tabs.Placement.RIGHT);
        tabs.addTab(Component.literal("Queue"), UiIcons.PLAY,
                new Label(Component.literal("Upcoming tracks")));
        tabs.addTab(Component.literal("Settings"), UiIcons.SETTINGS,
                new Slider(0, 100, 50, value -> { }));
        content.add(tabs);
        return new UiScreen(Component.literal("Example"), new Panel(content), UiTheme.WORKSHOP);
    }
}
