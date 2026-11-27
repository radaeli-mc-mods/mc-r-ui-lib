# Themes and visual tokens

R-Ui Lib provides the `UiTheme.DARK` and `UiTheme.LIGHT` themes. A consuming mod
can choose the global default once during client-side initialization:

```java
UiThemes.setGlobalTheme(UiTheme.LIGHT);
```

Screens created without an explicit theme, and all components that receive
their context, use this configuration. The global value can be updated at
runtime; open screens update their layout on the next render.
A screen can retain its own theme through the `UiScreen(title, root,
theme)` constructor. This theme overrides the global default for that screen only.

## Manual configuration

`UiPalette` is a public record with one argument for each semantic color,
using ARGB values:

| Token | Usage |
| --- | --- |
| `screen` | Screen background |
| `panel`, `card` | Panel and card surfaces |
| `border`, `divider` | Outlines and separators |
| `text`, `mutedText` | Primary and secondary text |
| `accent` | Selection, progress, and highlights |
| `button`, `buttonHover`, `buttonPressed` | Button states |
| `disabled`, `focus` | Disabled state and focus |
| `overlay` | Backdrop behind modals |
| `success`, `warning`, `error` | Toast states |

Other appearance tokens are grouped in `UiTokens`:

- Spacing: `panelPadding`, `cardPadding`, `buttonPadding`, and small,
  medium, and large gaps.
- Size: minimum button width and height.
- Typography: `small`, `body`, `heading`, and `title` scales.
- Corners: small, medium, large, and pill radii.
- Animation: default duration and easing (`LINEAR`, `EASE_IN_OUT`, or `EASE_OUT`).
  A duration of zero disables transitions.

This example keeps the Dark colors and replaces the other tokens:

```java
UiTokens customTokens = new UiTokens(
        Insets.all(12), Insets.all(8), 4, 8, 16,
        Insets.symmetric(12, 6), 72, 22,
        new UiTypography(0.9f, 1.0f, 1.2f, 1.35f),
        new UiRadii(2, 4, 8, 999),
        new UiMotion(180, UiEasing.EASE_OUT));
UiThemes.setGlobalTheme(new UiTheme(UiTheme.DARK.palette(), customTokens));
```

To define all colors manually, pass the 17 values to the `UiPalette`
constructor and use that record when creating the theme:

```java
UiPalette colors = new UiPalette(
        screen, panel, card, border, divider, text, mutedText, accent,
        button, buttonHover, buttonPressed, disabled, focus,
        overlay, success, warning, error);
UiThemes.setGlobalTheme(new UiTheme(colors, customTokens));
```

## Local customization

Pass another `UiTheme` to a screen's constructor to give it a theme independent
of the global default. Components also retain their local overrides, such as
colors in `Label` and `Divider`, colors in `ProgressBar`, and explicit padding
in `Panel`/`Card`. Without an override, their defaults come from the active
theme, including when the screen uses a custom global theme.

## Workshop theme

Available since R-Ui Lib 0.11.0.

`UiTheme.WORKSHOP` provides warm gray pixel frames, inset fields, olive selection, native Minecraft text at scale 1.0, and immediate state changes. Dark and Light retain their default painters and transitions. Pass the theme to an individual screen to keep other mods' global appearance unchanged.

```java
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
```
`Panel.setSurface` accepts DEFAULT, RAISED, INSET, PLAIN and TITLE_PLATE. Padding includes the border reserve. `Label.setOverflow` supports VISIBLE, CLIP, ELLIPSIS and WRAP with a maximum line count. Styled text is measured by the current Minecraft font.

`UiIcon` supports glyphs and texture regions; `UiIcons` supplies original pixel symbols. `IconButton.setIcon`, `setAccessibleName` and `setSelected` update a mounted control; selection alone does not execute its callback. Use snapshots owned by your mod as the source of truth. Textures must declare their full atlas dimensions and a valid region.

`Tabs.setPlacement` accepts TOP, LEFT and RIGHT; the complete tab strip is included in the component bounds. Side tabs use Up/Down; top tabs use Left/Right. Reserve the complete root through `jeiGuiBounds()` when composing a larger screen. `ScrollView.setSurface(PLAIN)` omits a nested frame.

Surface paint is delegated through `UiSkin`, with `UiSkinMetrics` and `UiVisualState`. The two-argument `UiTheme(palette, tokens)` constructor still uses the original painter. The record also exposes its skin: audit reflection/record serialization when integrating across versions. Disabled controls remain inactive while inspection can display their explanation.

Use the game's Font and ItemPreview; do not copy a font atlas or item texture into your mod to reproduce Workshop. Item colors, color swatches and mathematical chart values remain unchanged by the theme.

In the Style dialog, Panel padding reserves its frame and ProgressBar can hide its value label for thin tracks. Bounded Stepper arrows show independent availability; canStep(direction) reports it without executing the callback. Icon button names feed help text; this does not add full Minecraft narrator integration.
