# Changelog

## [0.8.0] - 2026-10-28

### Added

- Added radio buttons and vertical radio groups with hints, disabled options and keyboard navigation.
- Added steppers for bounded numbers and option lists, with arrows, mouse wheel and keyboard.
- Added compact information boxes that display help tooltips.
- Added position-aware tooltips, so a component can show its tooltip on part of its area.

## [0.7.0] - 2026-10-26

### Added

- Added selectable cards with independent child controls and keyboard focus.
- Added segmented selection with disabled options and keyboard navigation.
- Added built-in open and closed lock icons.

### Fixed

- Fixed custom tooltips on color swatches.

## [0.6.0] - 2026-10-26

### Added

- Added scalable item previews and selectable color swatches.
- Added an RGB and hexadecimal color picker with preview, apply and cancel.
- Added horizontal scrolling and optional local panel backgrounds.

### Fixed

- Fixed item previews appearing over modal dialogs.

## [0.5.0] - 2026-10-14

### Added

- Added Forge 1.20.1 support alongside NeoForge 1.21.1, with the same component API and development playground.
- Added a ready-to-load reference layout for every playground component category, available even before saving a layout.

### Fixed

- Fixed text selection by dragging and cursor timing in text fields across both supported loaders.

## [0.4.0] - 2026-10-09

### Added

- Added dropdown selection with option descriptions, keyboard navigation, mouse-wheel scrolling, and menus that stay visible outside scrolling panels.
- Added customizable slider readouts for percentages and other units.
- Added centered tab alignment and helpers for updating buttons and item pickers.

### Fixed

- Added a small gap between toast text lines for easier reading.

## [0.3.0] - 2026-10-07

### Added

- Added optional two-handle slider selection for ordered ranges, with mouse, scroll, keyboard, and playground editing support.
- Added optional interactive actions in modal headers.

### Changed

- Added more spacing between toast content and the left accent strip.
- Translated all documentation into US English and added a project README that clearly identifies the library as alpha and under construction.

### Fixed

- Fixed flickering at the end of toast and text fades, and kept toast accent strips inside rounded borders.
- Fixed text-field vertical alignment and clicks within field padding.
- Fixed internal tab corners so adjacent tabs form a continuous strip.
- Fixed themed tooltip layering over rendered items.
- Fixed row and column alignment and allowed screens to reserve their visible panel bounds for JEI.

## [0.2.0] - 2026-10-06

### Added

- Added `InventoryGrid` for rendering generic containers or the local player's main inventory and hotbar, with a picker mode that copies items into `ItemSlot` without changing the source inventory.
- Added custom interaction callbacks, configurable grid dimensions, and subtle hotbar highlighting for the player inventory layout.
- Added configurable `ItemSlot` backgrounds with named theme colors and arbitrary ARGB values, including playground inspector controls.
- Added a development example screen for testing inventory and JEI item drops into ghost slots.
