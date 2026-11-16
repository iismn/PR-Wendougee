# Crema — iPad landscape interface

The main use case is an iPad mounted horizontally beside the coffee machine. The interface keeps Crema's original content and screen routes, with a larger profile title, a dominant extraction chart, warm pressure and sage flow cards, neutral supporting readings, and a quiet action dock. The visual reference is the restrained typography, differentiated data cards and graduated controls observed in RonDesignLab's work; this is an original adaptation, not a claim of their authorship.

## Screen composition

- Main: profile header → extraction chart → four measurements → stage timeline → brewing/replay controls.
- The profile selector opens the existing library. Recipe creation, editing, sharing, community browsing, and settings remain there.
- History remains its original saved-shot list with yield/time/taste/timestamps, inline comments, and deletion actions.
- Recipe editing keeps visual/list mode, chart, recipe fields, grinder settings, and stages. On landscape widths the chart and recipe fields occupy the left side; stage selection and compact dial/rulers stay visible on the right. Narrow windows retain the stacked editor.
- Machine pairing and Auto-tune retain their existing presentations and command paths.
- No persistent sidebar, extra main recipe panel, or separate dashboard navigation is added.

## Visual and interaction details

- Official Pretendard faces/OFL license are bundled and registered in both app targets. Verified font names/hashes are in `Sources/CremaApp/Resources/Fonts`.
- Dot numerals retain the local LofreeHub reference glyphs and geometry. Lit-dot bounds are centered in both axes; numeral cells no longer bias values ending in1 or containing a decimal.
- Editor dials use a square ring, knob, and combined number/unit group with a shared exact center. Units align to the visible dot baseline. Rulers use a fixed centered caret and calibrated step spacing.
- Four 128-point minimum readouts form one row on landscape widths; compact widths use two columns with more vertical space, and accessibility sizes use one column. Small pressure/flow scales use actual telemetry fractions (12bar and8mL/s), not decorative data.
- Compact profile/history rows,44-point action targets, restrained press/expand/sheet feedback, spring control motion, keyboard/VoiceOver adjustment, and Reduce Motion support.
- Visible utility controls use icons; names remain in accessibility labels/tooltips. Decorative and implementation explanations are removed. Real recipe data, units, status, errors, and necessary confirmations remain.
- Auto appearance follows local06/12/18/23 boundaries and MonitorShade's smooth warmth curve. It refreshes every30seconds, on resume and timezone/day changes, with persistent manual overrides. Hardware display brightness is unaffected.

## Data and behavior

BLE/Modbus transport, extraction, telemetry and persistence paths are unchanged. Chart ranges, coordinate transforms, smoothing, targets and waits are retained. Apple CI exposed an existing sharing-API date decoder failure; the decoder now accepts ISO8601 dates with or without fractional seconds, with focused timestamp tests. Pumped water remains in mL; weight targets remain in g; absent telemetry is an em dash. Demo remains anchored to the bundled recording.

Parent-owned playback/driver objects are observed without retaining stale child-owned state. Ready-state recipe updates preserve the connection; completed telemetry keeps its original recipe until cleared. Profile/history viewing remains available while brewing, as in the original app.

## Review and validation

Live companion: http://127.0.0.1:4180 (`node outputs/ui-preview-server.cjs`). It listens only on this computer and reloads after preview saves. The HTML companion is a review reconstruction, not a native simulator render, and sends no hardware/account/backend requests.

Apple CI compiles both native targets and runs CremaKit tests. The isolated Chromium job checks the HTML companion at 1194×834 in day/night, 1024×768 and390×844, including overflow, the four-column landscape HUD, unobscured stages, library/editor entry, a6.1bar keyboard adjustment and cancelled-edit semantics. Screenshots are saved as a review artifact. HTML render checks do not establish native iPad layout or physical Bluetooth behavior.

The user stopped the exhaustive Instagram audit and requested implementation. Existing observations remain in `outputs/design-audit`; no exhaustive-review claim is made.
