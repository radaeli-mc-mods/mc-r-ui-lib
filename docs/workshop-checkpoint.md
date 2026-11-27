# Workshop implementation checkpoints

Branch: codex/workshop-theme. Base: b38a25e. User-owned docs/logo.png and docs/logo.psd excluded.
Reference: docs/workshop-reference.png. Visual approval remains with the user. No manual context-compaction tool is exposed; these checkpoints preserve phase boundaries.

## Foundation

Reference image reopened and reviewed. Added Workshop preset, skin dispatch, shared pixel metrics and visual state, third playground choice, reference snapshot and compatibility tests. No changes to the old theme constructors or render paths. Next: Phase 1.

## Phase 1 — surfaces, text and layouts

Reference image reopened and reviewed. Added semantic Panel variants, raised/inset/title frames, native styled overflow and divider grooves. Row/Column/Grid/Stack/AbsoluteLayout/Spacer retain transparent layout. Updated layout/basic presets and normalized optional fields. Next: Phase 2.

## Phase 2 — actions and selection

Reference image reopened and reviewed. Added original pixel icons and texture/glyph abstraction, selection/name/icon setters, compact pressed content, rectangular toggle/chip, pixel radio and keyboard-focusable help. Updated Basic preset and node icon fields. Next: Phase 3.

## Phase 3 — inputs and configuration

Reference image reopened and reviewed. Unified input/popup colors and frame, pixel search/dropdown symbols, styled popup ellipsis, rectangular slider/stepper, contiguous segments and unmodified RGB swatches. Invalid hex input is marked without changing Apply/Cancel. Updated Input examples. Next: Phase 4.

## Phase 4 — items and tabular data

Reference image reopened and reviewed. Added consistent inset inventory/ghost slots, selected child contrast, safe item/table text, alternating table rows and a built-in InventoryGrid picker. ItemGrid inherits themed cards; ItemPreview keeps the native renderer without an automatic frame. Next: Phase 5.

## Phase 5 — navigation and viewports

Reference image reopened and reviewed. Added icon side tabs within complete input bounds, keyboard direction and per-tab help, shared scrollbar materials and gutters, and Style dialog for surface/overflow/icons/tab placement. ClipStack remains transparent with native scissor nesting. Next: Phase 6.

## Phase 6 — progress, charts and radial

Reference image reopened and reviewed. Added inset progress preserving read-only behavior, dark plot/grid and native labels, square point/tangent handles with dashed tangents, and stepped radial edge material. Mathematical models, editing and custom curve colors preserved. Next: Phase 7.

## Phase 7 — overlays and help

Reference image reopened and reviewed. Added titled dialogs, compact level-labelled toasts, dark pixel tooltips, disabled inspection and keyboard help; preview shares native popup/focus handling. Added Select and overlay actions to the playground plus Workshop showcase. Next: real-client validation, documentation and final gate.

## Final gate

Reference image reopened and reviewed. Implemented all groups and revised all five showcase tabs after user screenshots. buildAll and 13 JUnit tests per loader passed; the API example compiles on both. Each real client with JEI passed 383 checks and produced 187 captures. Strict comparison: 182/187 identical, 5 inspected native 3D block-preview differences; strict command remains failing and this limit is documented. Portal check/build passed. Manual visual/font-pack/IME/narrator acceptance remains pending. See workshop-validation.md and workshop-previews/. Next: final phase-7 commit and user review.
