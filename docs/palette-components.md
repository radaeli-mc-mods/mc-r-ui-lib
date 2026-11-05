# Color and preview components (0.6.0)

`InfoBox` displays a compact question mark with a tooltip for inline help:

```java
InfoBox help = new InfoBox(Component.literal("Choose a value for this setting"));
```

Shared API for Forge 1.20.1 / Java 17 and NeoForge 1.21.1 / Java 21:

```java
ItemPreview preview = new ItemPreview(stack, 64);
ColorSwatch swatch = new ColorSwatch(0x4e91bd, false, () -> selectColor());
ColorPicker picker = new ColorPicker(0x4e91bd, rgb -> applyColor(rgb), () -> cancel());
HorizontalScrollView columns = new HorizontalScrollView(6);
Panel column = new Panel(content).setBackground(0xff4e91bd);
column.setPreferredSize(140, null);
columns.add(column);
```

`ItemPreview` copies its stack, accepts sizes 1–512 and scales the vanilla item
renderer. `UiScreen` renders its tooltip after clipping containers have finished.
Use `renderTooltip(context)` explicitly when drawing previews outside `UiScreen`.

`ColorSwatch` masks RGB to 24 bits and supports click, Enter and Space. The caller
owns selection state through `setSelected`; a hex tooltip identifies the color.

`ColorPicker` owns uncommitted preview state. RGB sliders and `#RRGGBB` input stay
in sync. Malformed hex disables Apply; only Apply invokes the commit consumer.
Cancel invokes its callback without committing. Host it in a modal at least 250 GUI
pixels high, or a vertically scrolling region when less height is available.

`HorizontalScrollView` lays out fixed-width children, clips rendering and hit tests,
and supports wheel, Left/Right keys and scrollbar dragging. Tab visits the viewport
and its visible child controls. `setScrollOffset` clamps to the content range.

`Panel.setBackground(null)` restores the normal theme panel. Existing constructors
and defaults are unchanged. `Button.label()` returns its current display label. `Label.setText` updates dynamic messages; relayout the parent when its natural size should change.

Playground categories Input, Data, Layout and Scroll include representative examples.
The opt-in `-PuiValidation` client suite exercises color preview/apply/cancel, invalid
hex, keyboard slider changes, horizontal wheel/keyboard/drag, and all bundled layouts.

## Grid composition (0.7.0)

`SelectableCard(content, selected, onSelect)` is a neutral frame around arbitrary
content. Passive content selects the frame; focusable child controls receive input
without bubbling. Tab visits the frame and its children. Selection uses the outer
accent border; keyboard focus uses a separate inner border.

`SegmentedControl<T>(options, value, onChange)` owns one exclusive value. Options
carry a value, label and optional enabled flag. Mouse commits on release; Left/Right
skip disabled options, Home/End select the first/last enabled option. `setValue`
updates without invoking callbacks. Enter/Space keep the current selection.

`IconButton` also accepts `Symbol.LOCK_OPEN` or `Symbol.LOCK_CLOSED`. These are
vector icons and do not depend on the Minecraft font containing emoji.

Screens that rebuild their trees can read `focusedComponent()` and use
`focusComponent(replacement)` after layout to preserve visible keyboard focus.

## Radio choices (0.8.0)

`RadioGroup<T>(options, value, onChange)` is the vertical counterpart of
`SegmentedControl`: one exclusive value, shown as rows with a round mark. The value may
be `null` until the player picks. Options carry a value, label, optional right-aligned
hint and enabled flag. Mouse commits on release; Up/Down skip disabled options and
commit immediately, Home/End select the first/last enabled option. `setValue` updates
without invoking callbacks. An optional `gap` separates rows.

`Radio(label, [hint], selected, onSelect)` is a single row for layouts that interleave
options with other content. Clicking selects but never clears it; the caller clears the
siblings. The mark is `Radio.INDICATOR` (8 px) in both states: selection fills the ring
instead of swapping glyphs, so labels never shift. `UiPaint.radio(...)` draws the same
mark for immediate-mode screens.

## Stepper (0.8.0)

`Stepper<T>` is a one-line picker: muted label on the left, the value against the right edge
between `‹` and `›`. `Stepper.ofRange(label, min, max, step, value, format, onChange)` moves a
bounded integer (Shift multiplies by `setShiftMultiplier`, 10 by default);
`Stepper.ofOptions(label, options, value, format, empty, wrap, onChange)` cycles a list and may
start at `null` (shown as `empty`) until the player picks: forward picks the first option,
backward the last. Arrows, the mouse wheel anywhere on the row and Left/Right/Up/Down (when
focused) step the value; `setValue` skips the callback. The tooltip only shows over the label
(`tooltipAt`), so it never covers the value or the arrows. Immediate-mode screens draw the
same row with `Stepper.paint(...)` and take click areas from `Stepper.layout(...)`.

`UiComponent.tooltipAt(mouseX, mouseY)` lets any component limit its tooltip to part of its
area; screens use it instead of `tooltip()`.
