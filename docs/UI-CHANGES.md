# GeeFlow iPad interface

This repository now uses [drobekk/GeeFlow](https://github.com/drobekk/GeeFlow) as its application base. The previous Crema source and HTML reconstruction are no longer part of the application. Upstream Git ancestry, GPL-3.0 license and authorship remain preserved.

The primary display is a landscape iPad. GeeFlow's device status, boiler readings, charts, chart toggles, profile/history list, editing, free control, cleaning and BLE services remain the source of behavior.

UI changes:

- Pretendard Regular/Medium/SemiBold/Bold across shared Compose screens, with tabular numerals. Fonts come from the official [Pretendard distribution](https://github.com/orioncactus/pretendard/tree/main/packages/pretendard/dist/public/static); license in `docs/licenses/Pretendard-OFL.txt`.
- Korean resources cover every shared UI string and plural, including settings, connection errors, recipe/step editors, maintenance and onboarding. iOS and Android register Korean as an app language; iOS permission prompts are also translated. Existing user-authored recipes and names remain intact. Linked upstream terms, privacy and third-party licenses retain their original language.
- Cool white surfaces with red-orange ambient light by day; a black base with deep navy light at night. Radial background gradients transition over 1.2 seconds at local time boundaries. Translucent chart, library and statistic tiles use graded opacity and fine reflective borders. This is shared Compose translucency, not an OS-specific backdrop-blur API. Selected recipes animate between surfaces and retain swipe actions and drag behavior.
- Horizontal controls have a fine ruler aligned with the drag range, a contrasting indicator and a tap action for exact numeric input. Accessibility adjustments clamp to the supported range; disabled controls do not send changes.
- Landscape chart/library proportions adapt to width; the profile column stays between 280 and 360 dp. Five existing brew statistics remain grouped by pressure, rates and totals, with compact dot numerals.
- New **Time of day** appearance setting, enabled for new users. Local 06:00/12:00/18:00/23:00 boundaries select morning/day/evening/night. The clock is rechecked on resume and every30 seconds, including timezone changes. Existing System/Light/Dark settings remain available. This changes app colors, not hardware brightness.
- Screenshots are rendered from actual shared Compose components via `generateStoreScreenshots`, including dashboard, profile editor and step editor in day/night, a smaller landscape dashboard, and phone layouts. These are JVM Compose renders, not iPad simulator captures.

Screenshots use the Korean locale. Demo fixtures are GeeFlow's own preview data, not measurements from the user's machine. `scripts/check-korean.ps1` checks resource parity, XML and format placeholders.
