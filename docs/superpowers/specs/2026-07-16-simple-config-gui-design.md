# Simple Config GUI Design

## Problem

Aether's existing configuration screen is rendered with NanoVG. On Android/Pojav, the macro can run, but opening that screen can crash the game. Users need an opt-in configuration screen built only from vanilla Minecraft GUI components so it does not initialize or render NanoVG.

## Goals

- Let users enable the simple GUI before opening the existing GUI.
- Persist the selected GUI mode across restarts.
- Keep `/aether` and the existing GUI keybind working in either mode.
- Expose settings used to operate farming, other automation, and failsafes.
- Reuse `MainGUIRegistry` so both GUIs edit the same settings and invoke the same setters.
- Avoid NanoVG on the entire simple-GUI path.

## Non-goals

- Reproduce the look, animations, search, profiles, or layout of `MainGUI`.
- Expose colors, themes, visuals, HUD positioning, profiles, or cosmetic keybind customization.
- Automatically detect Android/Pojav.
- Replace the existing custom GUI.

## Mode Selection and Commands

Add a persistent `simpleConfigGui` boolean entry to `AetherConfig`, defaulting to `false`.

- `/aether simple` sets the entry to `true`, saves it, and immediately opens `SimpleConfigScreen`.
- `/aether custom` sets the entry to `false`, saves it, and immediately opens the existing `MainGUI`.
- `/aether` and the existing Insert key open the screen selected by `simpleConfigGui`.
- Closing the currently selected config screen through the existing toggle action returns to the game.

The custom GUI's Bootstrap settings gain a `Simple Config GUI` toggle. The simple screen has a visible `Use Custom GUI` button that persists `false` and opens `MainGUI`, so users can switch back without typing a command.

## Screen Architecture

`SimpleConfigScreen` extends Minecraft's vanilla `Screen` and uses only vanilla text rendering and widgets. It must not extend `NVGScreen`, call `NanoVGManager`, or reference NanoVG rendering helpers.

Before building its rows, the screen calls `MainGUIRegistry.refresh()`. It reads only these module section IDs:

- `farming`
- `other`
- `failsafes`

It does not read `COLORS_SUBTABS`, `PROFILES_SUBTABS`, `KEYBINDS_SUBTABS`, `SETTINGS_SUBTABS`, or the `visuals` module section. This includes operational Discord and mining settings registered under `other` while excluding the dedicated visual section.

Navigation has three lightweight levels:

1. Operational section list.
2. Module/subtab list within the selected section.
3. Scrollable groups and settings for the selected module.

Each level uses vanilla buttons plus Back/Done controls. Screen changes rebuild the visible widget list so existing `isVisible()` conditions are reevaluated immediately.

## Setting Controls

The simple GUI renders existing `Setting` objects without copying their configuration logic:

- `ToggleSetting`: on/off cycle button.
- `SliderSetting`: vanilla slider with the configured range, precision, and suffix.
- `RangeSliderSetting`: separate lower and upper vanilla sliders that preserve ordering.
- `TextSetting`: vanilla text field; commit on Enter, focus loss, or Done.
- `DropdownSetting`: cycle button over its existing options.
- `DropdownListSetting`: vanilla child editor for the existing list of dropdown selections.
- `MultiDropdownSetting`: vanilla on/off rows for each existing option.
- `ListSetting`: vanilla child editor with add, edit, and remove operations.
- `ActionSetting`: ordinary action button.
- `InfoSetting`: wrapped read-only text.

Module and group enable states use ordinary on/off buttons. Changing a value calls the setting's existing setter, so persistence and side effects remain identical to `MainGUI`.

`ColorSetting`, `PositionSetting`, and `KeybindSetting` are intentionally not rendered. Any future unknown setting type in an operational section is shown as a disabled `Not supported in Simple GUI` row instead of being silently omitted.

## Lifecycle and Failure Handling

Opening `SimpleConfigScreen` stops a running macro with the same reason and close-screen behavior as `MainGUI`.

GUI routing is centralized in `AetherUiActions`; commands and keybinds do not construct screens directly. Registry refresh or screen-construction failures are logged and reported to chat through the existing error path. A failure to open the custom GUI does not automatically retry it because native Android crashes may not be catchable; users can run `/aether simple` after restarting.

Text and list editors preserve their pending input while open and commit before navigating away. They delegate validation to the existing setter; if a setter rejects a value, rebuilding the row displays the unchanged stored value.

## Verification

Automated checks cover logic that does not require rendering:

- GUI mode is persisted when the simple/custom command action runs.
- `/aether` and the keybind route to the selected screen type.
- Only `farming`, `other`, and `failsafes` sections are included.
- Visual sections and color/position/keybind setting types are excluded.
- Supported setting types use their existing getters and setters.
- Range controls cannot cross their lower/upper bounds.

A full Gradle build verifies compatibility with the current Minecraft mappings and Fabric API. Final Android verification is a device smoke test: run `/aether simple`, edit and persist representative farming/failsafe settings, close and reopen with both `/aether` and Insert, restart the client, then run `/aether custom` to confirm switching remains reversible.

## Acceptance Criteria

- Android/Pojav users can run `/aether simple` without opening NanoVG-backed `MainGUI` first.
- The simple screen opens through vanilla GUI code and does not crash due to NanoVG initialization.
- Operational macro and failsafe settings are editable and persist through the existing config setters.
- Cosmetic/customization settings are absent.
- `/aether custom` restores the original GUI, while `/aether` and Insert honor the saved preference.
