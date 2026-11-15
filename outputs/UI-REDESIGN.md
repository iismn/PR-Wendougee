# Crema UI refinements

The interface keeps Crema's original screen composition and content flows. The changes focus on Pretendard typography, time-aware coffee palettes, alignment, compact lists, and small interaction feedback.

## Screen composition

- Main: profile header → extraction chart → four measurements → stage timeline → brewing/replay controls.
- The profile selector opens the existing library. Recipe creation, editing, sharing, community browsing, and settings remain there.
- History remains its original saved-shot list with yield/time/taste/timestamps, inline comments, and deletion actions.
- Recipe editing keeps the original visual/list mode, chart, recipe fields, grinder settings, and stages. Dials and rulers are confined to the stage editor.
- Machine pairing and Auto-tune retain their existing presentations and command paths.
- No persistent sidebar, extra main recipe panel, or separate dashboard navigation is added.

## Visual and interaction details

- Official Pretendard faces/OFL license are bundled and registered in both app targets. Verified font names/hashes are in `Sources/CremaApp/Resources/Fonts`.
- Dot numerals retain the local LofreeHub reference glyphs and geometry. Lit-dot bounds are centered in both axes; numeral cells no longer bias values ending in1 or containing a decimal.
- Editor dials use a square ring, knob, and combined number/unit group with a shared exact center. Units align to the visible dot baseline. Rulers use a fixed centered caret and calibrated step spacing.
- Four equally sized110-point minimum readouts form one row when space permits and2×2 on compact widths; accessibility sizes use one column.
- Compact profile/history rows,44-point action targets, restrained press/expand/sheet feedback, spring control motion, keyboard/VoiceOver adjustment, and Reduce Motion support.
- Visible utility controls use icons; names remain in accessibility labels/tooltips. Decorative and implementation explanations are removed. Real recipe data, units, status, errors, and necessary confirmations remain.
- Auto appearance follows local06/12/18/23 boundaries and MonitorShade's smooth warmth curve. It refreshes every30seconds, on resume and timezone/day changes, with persistent manual overrides. Hardware display brightness is unaffected.

## Data and behavior

BLE/Modbus transport, extraction, telemetry and persistence paths are unchanged. Chart ranges, coordinate transforms, smoothing, targets and waits are retained. Apple CI exposed an existing sharing-API date decoder failure; the decoder now accepts ISO8601 dates with or without fractional seconds, with focused timestamp tests. Pumped water remains in mL; weight targets remain in g; absent telemetry is an em dash. Demo remains anchored to the bundled recording.

Parent-owned playback/driver objects are observed without retaining stale child-owned state. Ready-state recipe updates preserve the connection; completed telemetry keeps its original recipe until cleared. Profile/history viewing remains available while brewing, as in the original app.

## Review and validation

Live companion: http://127.0.0.1:4180 (`node outputs/ui-preview-server.cjs`). It listens only on this computer and reloads after preview saves. The HTML companion is a review reconstruction, not a native simulator render, and sends no hardware/account/backend requests.

Source checks confirm original main order, four existing presentations, and41 checked library/history/editor routes or bindings. `git diff --check` passes. Native compilation, iOS/macOS layout, and physical LITA Bluetooth validation require Xcode on a Mac; they have not been run on this Windows host.

The user stopped the exhaustive Instagram audit and requested implementation. Existing observations remain in `outputs/design-audit`; no exhaustive-review claim is made.
