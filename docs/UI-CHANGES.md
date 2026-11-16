# GeeFlow iPad interface

This repository now uses [drobekk/GeeFlow](https://github.com/drobekk/GeeFlow) as its application base. The previous Crema source and HTML reconstruction are no longer part of the application. Upstream Git ancestry, GPL-3.0 license and authorship remain preserved.

The primary display is a landscape iPad. GeeFlow's device status, boiler readings, charts, chart toggles, profile/history list, editing, free control, cleaning and BLE services remain the source of behavior.

UI changes:

- Pretendard Regular/Medium/SemiBold/Bold across shared Compose screens, with tabular numerals. Fonts come from the official [Pretendard distribution](https://github.com/orioncactus/pretendard/tree/main/packages/pretendard/dist/public/static); license in `docs/licenses/Pretendard-OFL.txt`.
- Neutral porcelain surfaces, charcoal actions, restrained chart colors and consistent corner radii. Selected profiles animate between surfaces and keep their existing swipe actions and drag behavior.
- Landscape chart/library proportions adapt to width; the profile column stays between 280 and 360 dp. Five existing brew statistics remain grouped by pressure, rates and totals, with compact dot numerals.
- New **Time of day** appearance setting, enabled for new users. Local 06:00/12:00/18:00/23:00 boundaries select morning/day/evening/night. The clock is rechecked on resume and every30 seconds, including timezone changes. Existing System/Light/Dark settings remain available. This changes app colors, not hardware brightness.
- Screenshots are rendered from actual shared Compose components via `generateStoreScreenshots`, including dashboard, profile editor and step editor in day/night, a smaller landscape dashboard, and phone layouts. These are JVM Compose renders, not iPad simulator captures.

Demo fixtures in screenshots are GeeFlow's own preview data, not measurements from the user's machine.
