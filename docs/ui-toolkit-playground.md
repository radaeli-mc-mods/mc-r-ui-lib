# UI Toolkit playground

The playground is a development tool for experimenting with toolkit components inside Minecraft.

## Opening the playground

- Start the client through `runClient` and press **F7**.
- Press **F8** to open the `ItemSlot` example screen, with `InventoryGrid` and JEI drag integration. The shortcut is configurable in Controls.
- Use **Theme** in the top toolbar to choose **Dark**, **Light**, or **Workshop**; this changes the global theme while the playground is open.
- The key can be changed under **Options → Controls → R-Ui Lib**.
- In development runs, **F6** displays the test toast sequence over the game. This key can also be changed in Controls.
- The screen is registered only in development runs. It is not available in production installations of the mod.
- **Toast** cycles through the four notification levels. Notifications in the upper-right corner disappear after four seconds; left-click a notification to dismiss it.
- **Table** provides sample rows, selection, and vertical scrolling. In the inspector, edit the comma-separated headers and the number of columns.
- **ProgressBar** displays determinate progress. In the inspector, configure the range as `minimum, maximum, value` and switch between displaying the value and the normalized percentage. Values are rounded to integers; the percentage ranges from `0%` to `100%`, regardless of the range. The API supports height, track/fill colors, a formatter, and percentage mode. In Preview, the example continuously moves from minimum to maximum and back; returning to Edit restores the configured value.
- **RadialMenu** starts with four segments. In the inspector, edit the comma-separated labels; in Preview, press, drag to a sector, and release to trigger the demonstration action. The center area cancels the selection.
- **Dialog** opens a demonstration `Modal`. It is centered over the screen, contains focus and input, closes with Escape or its internal button, and also accepts outside clicks when enabled.

## Building a layout

- Drag a type from the palette on the left onto the canvas. It contains `Label`, `Button`, `IconButton`, `Panel`, `Card`, `Divider`, `Toggle`, `Chip`, `TextField`, `SearchField`, `Slider`, `ProgressBar`, `RadialMenu`, `ItemCard`, `ItemSlot`, `Tabs`, `ScrollView`, `ClipStack`, `Row`, `Column`, `Stack`, `Grid`, `ItemGrid`, `Table`, `Spacer`, and `AbsoluteLayout`. The list scrolls with the mouse wheel in short windows. `ItemRenderer` is a helper used by `ItemCard` and `ItemSlot`, not a separate node.
- `Panel` and `Card` accept children; nest a `Row`, `Column`, or `Grid` to define the inner layout. `Card` also exposes a selected state in the inspector. `IconButton` uses the first character of its name as its icon and shows the name in a themed tooltip. Item tooltips continue to use the vanilla renderer. `Divider` is horizontal in the playground.
- Select a node to edit its text, color, position, dimensions, spacing, and alignment in the inspector; press **Apply** to apply the fields. Drag the lower-right corner to resize; **Delete** or **Remove** deletes the node.
- Drop a component onto `Row`, `Column`, or `Stack` to nest it. The container then controls its children's positions; X/Y become inactive, while width and height remain adjustable.
- Drop `ItemCard` onto `Grid` or `ItemGrid` to build a grid. The inspector edits the grid's columns and spacing, and the card's namespaced item ID and count. `Spacer` can be fixed (adjust Width/Height) or flexible (the Flex button in the inspector), filling the remaining space in `Row`/`Column`.
- `ItemSlot` accepts items dragged from JEI when JEI is installed. The selected item is a filter reference and does not enter an inventory; right-click, Delete, or Backspace clears the slot. In the F8 example screen, the player grid lets you click or drag items into slots without removing them from the inventory.
- Drag a child onto an empty area of the canvas to move it back into `AbsoluteLayout`.

## Testing and saving

- Switch between **Preview** and **Edit** in the top toolbar. In Preview, the palette and inspector disappear and the canvas fills the available width; the top toolbar remains accessible for returning to Edit or closing. Drag with the middle mouse button in **Edit** and **Preview** to pan the canvas; this does not change saved positions, and the pan is preserved when switching modes. Buttons, toggles, and chips respond to the mouse, Tab/Shift+Tab, Enter, and Space. `Tabs` changes pages by clicking or using the left/right arrows while focused. Interactions update the visible status/counter.
- Drop children inside `Tabs` to create pages; the first child's text serves as the tab title. Add two or more pages to navigate between them.
- Text fields support editing in Preview; `TextField` and `SearchField` retain the value saved in the layout. The search field displays a magnifying glass, and both vertically center their text with at least 8 px of left padding. `ScrollView` stacks children vertically, scrolls with the mouse wheel, and supports dragging its scrollbar. `ClipStack` overlays its children and clips any drawing outside the container.
- `Slider` can be adjusted by dragging, scrolling, or using the keyboard. In the inspector, edit the range as `minimum, maximum, step`; use a step of `0` for continuous values. `Value` sets the initial value; fill in `Upper` to enable the second handle or leave it empty to use a single handle. For example, use the range `0, 1000, 1`, a `Value` of `10`, and an `Upper` of `100`. The second handle always remains above the first. Clicking or scrolling adjusts the handle closest to the mouse; while focused, Space switches handles, and arrows, Home/End, and Page Up/Down adjust the selected handle. Both values are saved in JSON.
- `ProgressBar` is visual only and displays the current value rounded without decimal places; its values can be edited as `minimum, maximum, value`. Use the `Format` button in the inspector to display the normalized percentage from `0%` to `100%`. In Preview, the value demonstrates the continuous progress cycle.
- Enter a name in the top text box and use **Save** to write to `config/r_ui_lib/ui-playground/` inside the game directory. **Load** always lists the bundled `Basic`, `Input`, `Layout`, `Data`, `Status`, and `Scroll` reference layouts, followed by saved layouts; select an entry and confirm. The bundled examples load directly from the mod and are available even before any layout has been saved.
- When the playground opens, the last saved layout loads automatically. If older layouts have no record of the last save, the most recently modified JSON file is used.
- **New** creates an empty document. Demonstration callbacks are recreated locally and are never written to JSON.
- File or format errors appear in the screen's bottom bar.

Documents use `version: 1`; the playground rejects unknown versions and structurally invalid layouts without closing the game.

## Review Workshop in the playground

Press F7 in a development client, choose **Theme → Workshop**, then **Load → Workshop — Reference composition**. Enable **Preview** to use the controls. Built-in layouts work before any Save; user layouts are preserved. Review Basic, Input, Layout, Data, Status and Scroll with the same theme. Inventory picker, Select, help, dialog and toast examples are included.

In Edit mode select a component and use **Style** to choose surface variant, text overflow/line count, icon, selected state or tab placement where applicable. Toolbars and inspector fields include native Minecraft editor widgets; the canvas preview contains the library components. The theme picker changes the global theme of the development session, while screens with explicit overrides keep their own theme.

Style also exposes Panel padding and a value-label switch for thin ProgressBar tracks.
