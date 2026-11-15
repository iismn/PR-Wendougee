# Crema interface preview

Open `index.html` in a browser, or use `http://127.0.0.1:4180` while the local review server is running. CSS, Pretendard Variable, JavaScript, SVG controls and recorded telemetry are embedded. The file works offline and makes no hardware, Bluetooth, authentication or backend requests.

This is an HTML review companion, not a rendered native build. Native presentation, platform accessibility, haptics and drawing still require Xcode/device validation.

The original Crema composition is preserved:

1. Profile selector, journal, machine, appearance and Live/Demo controls in the header.
2. Extraction chart as the main content, growing into available viewport height.
3. Four readings: pressure, pump flow, pumped water volume and elapsed time. Wide layouts use one row; narrow layouts use two columns.
4. Stage timeline, followed by a sticky brew/replay action zone.

There is no permanent sidebar, four-tab navigation, large pressure gauge or quick recipe editor on the main screen.

The profile selector opens the recipe library as a modal. Its compact vertical rows retain target/time/stage metadata, curve previews, selection, editing, sharing and deletion. Add, community and settings retain their original entry points. Recipe editing opens a separate modal with mode/preview, recipe fields, grinder settings and stages. The dial and calibrated duration ruler are confined to that editor. Preview changes persist locally; explicit Save applies them to the local library.

Sharing exports a local `.crema` file using the source `ShareableProfile` version1 envelope and BrewProfile fields. Community/account operations require the native app and are not simulated. Machine connection is an honest unpaired state. Journal starts empty; existing preview-local records can be edited or deleted. The demo recording is never silently added as a saved shot.

Data and controls:

- 104 original brew rows from `Sources/CremaApp/Resources/brew_testyt_full.csv`, excluding its two pre-brew rows as the native loader does.
- Flow uses the native loader's seven-sample centered volume derivative. Replay current readings use native linear interpolation.
- The internal TestyT profile remains paired with its recording: Preinfuse7s +8s wait, Soak3s, Extract12s at1.7mL/s and Tail0s. Profile duration30s; recording end30.3s. Target68mL means pumped water, not measured cup yield.
- Library defaults remain Classic Espresso34s/36mL/3stages and Turbo15s/54mL/2stages.
- Recipe editor defaults follow EditableProfile. Dose is an editor input, not a measured telemetry value; no dose or cup yield is fabricated on the brew screen.
- The editor duration ruler uses the native1–60s range,0.5s step and8px tick pitch.40px of dragging changes duration by2.5s. Dial, ruler and graph controls retain pointer, wheel and keyboard access.
- Dot glyphs/pitch/radius/spacing match the user's Lofree Hub source. The entire lit path is optically centered without stretching its dots or lighting unlit cells.
- Row hover/press, modal presentation, numeric dot changes and control settling use targeted motion, with Reduced Motion respected.

Auto appearance follows the browser's local clock, refreshing at least every30 seconds and on resume: Morning06–12, Day12–18, Evening18–23, Night23–06. MonitorShade's smooth05–09 cool-down and14–20 warm-up are retained. Manual phases persist locally. Only UI colors change.

Validation: JavaScript syntax, recording integrity, source glyph parity and schedule boundaries were checked. Root browser review covers responsive layout, modal entry points and local interactions. Native compilation remains a separate macOS Xcode check.
