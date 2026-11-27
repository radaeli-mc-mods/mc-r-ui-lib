package com.radaeli.ruilib.ui;
/** Optional semantic painters. Returning false preserves the original R-ui painter. */
public interface UiSkin {
    UiSkin DEFAULT = new UiSkin() { };
    default boolean pixelated() { return false; }
    default UiSkinMetrics metrics() { return UiSkinMetrics.DEFAULT; }
    default boolean surface(UiContext c, Bounds b, SurfaceVariant variant, Integer fill) { return false; }
    default boolean button(UiContext c, Bounds b, UiVisualState state) { return false; }
    default int buttonText(UiTheme theme, boolean enabled, boolean selected) {
        return enabled ? theme.palette().text() : theme.palette().mutedText();
    }
    default int inputText(UiTheme theme, boolean enabled) {
        return enabled ? theme.palette().text() : theme.palette().mutedText();
    }
}
