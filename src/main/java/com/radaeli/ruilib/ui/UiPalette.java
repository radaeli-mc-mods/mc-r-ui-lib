package com.radaeli.ruilib.ui;

/** Semantic ARGB colors used by toolkit primitives. */
public record UiPalette(
        int screen,
        int panel,
        int card,
        int border,
        int divider,
        int text,
        int mutedText,
        int accent,
        int button,
        int buttonHover,
        int buttonPressed,
        int disabled,
        int focus,
        int overlay,
        int success,
        int warning,
        int error
) {
    /** Derives contrast for children on a selected surface without changing other roles. */
    public UiPalette withText(int primary,int secondary) {
        return new UiPalette(screen,panel,card,border,divider,primary,secondary,accent,button,buttonHover,buttonPressed,disabled,focus,overlay,success,warning,error);
    }
    /** Keeps source compatibility with the first public palette shape. */
    public UiPalette(int screen, int panel, int card, int border, int divider, int text, int mutedText,
                     int accent, int button, int buttonHover, int buttonPressed, int disabled, int focus) {
        this(screen, panel, card, border, divider, text, mutedText, accent, button, buttonHover,
                buttonPressed, disabled, focus, 0x99000000, 0xFF63C78D, 0xFFE8B64F, 0xFFE56B73);
    }
}
