package com.radaeli.ruilib.ui;
/** Persistent selection is independent of transient pressure and keyboard focus. */
public record UiVisualState(boolean enabled, double hover, double pressed, double selected, double focused) { }
