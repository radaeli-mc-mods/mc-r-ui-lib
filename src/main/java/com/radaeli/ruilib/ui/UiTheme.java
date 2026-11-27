package com.radaeli.ruilib.ui;

import java.util.Objects;

/** Immutable bundle of semantic colors and design tokens. */
public record UiTheme(UiPalette palette, UiTokens tokens, UiSkin skin) {
    public UiTheme(UiPalette palette, UiTokens tokens) { this(palette, tokens, UiSkin.DEFAULT); }
    public static final UiTheme WORKSHOP = new UiTheme(
        new UiPalette(0x99000000,0xFFC6C4BC,0xFF97978B,0xFF242323,0xFF77756F,
            0xFF403D38,0xFF686457,0xFFB4C478,0xFF999A90,0xFFAAA99B,0xFF777D62,
            0xFF92918A,0xFFF8DFA0,0x99000000,0xFF6D814C,0xFFAA823D,0xFF965A48),
        new UiTokens(Insets.all(8),Insets.all(6),2,4,8,Insets.symmetric(4,3),20,20,
            new UiTypography(1,1,1,1),new UiRadii(0,0,0,0),new UiMotion(0,UiEasing.LINEAR)),
        new WorkshopSkin());

    public static final UiTheme DARK = new UiTheme(
            new UiPalette(
                    0xE6101319, 0xF01B2029, 0xF5222933, 0xFF3B4655,
                    0xFF35404D, 0xFFF0F3F7, 0xFFADB7C5, 0xFF67B7F7,
                    0xFF303A47, 0xFF3C4B5C, 0xFF26313D, 0xFF252A31,
                    0xFF8BCBFF, 0x99000000, 0xFF63C78D, 0xFFE8B64F, 0xFFE56B73),
            new UiTokens(Insets.all(8), Insets.all(6), 4, 8, 16,
                    Insets.symmetric(10, 5), 64, 20, UiTypography.DEFAULT,
                    UiRadii.DEFAULT, UiMotion.DEFAULT));

    public static final UiTheme LIGHT = new UiTheme(
            new UiPalette(
                    0xFFF3F5F8, 0xFFFFFFFF, 0xFFF8FAFC, 0xFFD1D8E2,
                    0xFFE1E6ED, 0xFF202936, 0xFF657184, 0xFF2563EB,
                    0xFFE8EDF5, 0xFFDCE6F5, 0xFFCCD9EB, 0xFFE4E7EC,
                    0xFF3B82F6, 0x660F172A, 0xFF18864B, 0xFF9A6400, 0xFFCE3346),
            new UiTokens(Insets.all(8), Insets.all(6), 4, 8, 16,
                    Insets.symmetric(10, 5), 64, 20, UiTypography.DEFAULT,
                    UiRadii.DEFAULT, UiMotion.DEFAULT));

    public UiTheme {
        Objects.requireNonNull(palette, "palette");
        Objects.requireNonNull(tokens, "tokens");
        Objects.requireNonNull(skin, "skin");
    }
}
