package com.radaeli.ruilib.ui;
/** Logical GUI units; appearance and hit testing share these metrics. */
public record UiSkinMetrics(int frameInset, int iconSize, int scrollbarWidth, int scrollbarHitWidth, int popupRowHeight) {
    public static final UiSkinMetrics DEFAULT = new UiSkinMetrics(1, 10, 4, 8, 20);
    public static final UiSkinMetrics WORKSHOP = new UiSkinMetrics(3, 9, 4, 8, 20);
}
