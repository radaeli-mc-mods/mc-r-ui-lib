package com.radaeli.ruilib.ui;
/** Original procedural pixel frames. No external mod or vanilla textures are bundled. */
public final class WorkshopSkin implements UiSkin {
    public static final int EDGE_LIGHT=0xFFF5F2E7, EDGE_DARK=0xFF77756F, INSET_LIGHT=0xFFE3E0D4,
        INSET_DARK=0xFF585750, TITLE=0xFFE4D6B9, DISPLAY=0xFF97978B, WELL=0xFF8B887E,
        ROW=0xFFB2AFA3, ROW_ALT=0xFFA8A59A, TEXT_BUTTON=0xFFF5F2E8,
        TEXT_SELECTED=0xFFDCF3A4, INPUT=0xFF6D6A60, INPUT_TEXT=0xFFF4EEE1,
        TOOLTIP=0xFF100B19, TOOLTIP_BORDER=0xFF56406F, PLOT=0xFF5F6159, PLOT_GRID=0xFF74766A;
    @Override public boolean pixelated() { return true; }
    @Override public UiSkinMetrics metrics() { return UiSkinMetrics.WORKSHOP; }
    @Override public int buttonText(UiTheme t, boolean enabled, boolean selected) {
        return !enabled ? 0xFF64655F : selected ? TEXT_SELECTED : TEXT_BUTTON;
    }
    @Override public int inputText(UiTheme t, boolean enabled) { return enabled ? INPUT_TEXT : 0xFFB0ADA3; }
    @Override public boolean surface(UiContext c, Bounds b, SurfaceVariant variant, Integer override) {
        int fill=override!=null?override:variant==SurfaceVariant.INSET?DISPLAY:
            variant==SurfaceVariant.TITLE_PLATE?TITLE:c.theme().palette().panel();
        UiPaint.rect(c,b,fill);
        if(variant==SurfaceVariant.PLAIN || b.width()<3 || b.height()<3)return true;
        UiPaint.outline(c,b,c.theme().palette().border());
        Bounds inner=b.inset(Insets.all(1));
        boolean inset=variant==SurfaceVariant.INSET;
        int light=inset?INSET_DARK:EDGE_LIGHT, dark=inset?INSET_LIGHT:EDGE_DARK;
        UiPaint.rect(c,new Bounds(inner.x(),inner.y(),inner.width(),1),light);
        UiPaint.rect(c,new Bounds(inner.x(),inner.y(),1,inner.height()),light);
        UiPaint.rect(c,new Bounds(inner.x(),inner.bottom()-1,inner.width(),1),dark);
        UiPaint.rect(c,new Bounds(inner.right()-1,inner.y(),1,inner.height()),dark);
        return true;
    }
    @Override public boolean button(UiContext c, Bounds b, UiVisualState s) {
        UiPalette p=c.theme().palette();boolean selected=s.selected()>=0.5;
        int fill=!s.enabled()?p.disabled():selected?p.buttonPressed():s.hover()>0?p.buttonHover():p.button();
        surface(c,b,s.enabled()&&(s.pressed()>=0.5||selected)?SurfaceVariant.INSET:SurfaceVariant.RAISED,fill);
        if(s.focused()>0&&s.enabled())UiPaint.outline(c,b.inset(Insets.all(2)),p.focus());
        return true;
    }
}
