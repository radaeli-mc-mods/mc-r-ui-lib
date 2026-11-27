package com.radaeli.ruilib.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Text input specialized for search queries. */
public final class SearchField extends TextField {
    public SearchField() {
        this(Component.literal("Search..."));
    }

    public SearchField(Component hint) {
        super(hint);
    }

    @Override
    protected int editorInsetLeft() {
        return 14;
    }

    @Override
    protected void renderPrefix(UiContext context) {
        if(context.theme().skin().pixelated()){
            UiIcons.SEARCH.render(context,bounds().x()+6,bounds().centerY()-3,context.theme().skin().inputText(context.theme(),isEnabled()));return;
        }
        GuiGraphics graphics = context.graphics();
        int x = bounds().x() + 7;
        int y = bounds().centerY() - 5;
        int color = isEnabled() ? context.theme().palette().mutedText()
                : context.theme().palette().disabled();

        // Small pixel magnifier with an explicit handle so this field is visually distinct from TextField.
        graphics.fill(x + 2, y, x + 6, y + 1, color);
        graphics.fill(x + 1, y + 1, x + 2, y + 5, color);
        graphics.fill(x + 6, y + 1, x + 7, y + 5, color);
        graphics.fill(x + 2, y + 5, x + 6, y + 6, color);
        graphics.fill(x + 6, y + 6, x + 8, y + 7, color);
        graphics.fill(x + 7, y + 7, x + 9, y + 8, color);
    }
}
