# Wendougee LITA iPad interface

This is a UI fork of [drobekk/GeeFlow](https://github.com/drobekk/GeeFlow). Upstream Git ancestry, authorship and GPL-3.0 licensing are preserved. The original README at the imported revision is retained in `UPSTREAM_README.md`.

## Profile home

The landscape home follows the supplied reference: four large readings across the top, a narrow pressure summary and profile list on the left, one large circular profile selector in the center, and a tall graph panel on the right. The small surrounding circles have been removed. The left list uses consistent rows within one surface, with a restrained selected state and an edit action. A stronger cool blue-gray gradient colors the right panel.

The selected circle retains its reference size and position. Fine perimeter ticks and an animated needle show brew progress; tapping it opens the existing editor. Curved controls beside the circle open quick settings, free control and detailed charts. The bottom-center navigation and formerly bottom-right brew controls share one glass bar. The brew controls do not create a second glass layer inside that bar.

Profile selection, editing, adding, history, brewing, stopping, cleaning and free control continue to use the existing dashboard/profile events and guards. Full charts, regular Pretendard readings and the original profile/history management remain accessible from the chart icon. Compact phone layouts are preserved.

## Shared workspaces

Profile editing and step editing share CoffeeWorkspace: the home's 20/12dp outer padding, 20dp gaps, left list width, metric strip and blue-gray right panel. Lists use 12sp, section headings 14sp, metric values 36sp, smaller values 27sp and units 12sp with baseline alignment. Action glyphs use a 20dp box and a 42dp touch target. Settings, forms and dialogs inherit the same text roles and surfaces. Nested step-condition and ramp controls are transparent within their parent panel, with subtle group outlines instead of multiple fill colors.

Quick boiler control uses the navigation dialog strategy and opens over the current screen. Brew and steam controls share one surface with no nested card fills. Each control retains its own machine-provided range and callback; interaction tests check independent routing, clamping and the disabled applying state. Existing confirm events still apply the values.

The app display name is Wendougee LITA on iOS, Android and desktop. Internal namespaces and GeeFlow attribution are retained. InstrumentNumber's dot glyph renderer was removed; brew metrics use ordinary Pretendard with baseline-aligned units. The screensaver alone uses monochrome dot digits with radial white glow and no date.

## Typography and appearance

Pretendard Regular/Medium/SemiBold/Bold is bundled with tabular numerals. Korean resources cover shared UI strings, plural forms, settings, permission prompts, errors and editors. User-authored names and descriptions remain unchanged. The font license is in `licenses/Pretendard-OFL.txt`.

A cool white base with red-orange ambient light transitions to black and deep navy at night. The local 06:00/12:00/18:00/23:00 phases are checked on resume and every 30 seconds. System, Light and Dark modes remain available. This changes app colors, not hardware brightness.

## Native materials

On iPadOS 26+, `NativeGlassViewController` hosts actual `UIVisualEffectView` objects with `UIGlassEffect(style: .regular)` beneath a transparent Compose view. They sample a UIKit ambient background. Window-point coordinates, removal and pressed state are sent from shared components. A weak relay prevents a controller retain cycle. Previous iPadOS versions use `UIBlurEffect(.systemMaterial)`.

The native effect views remain at full alpha. Content text and graphs are painted above the materials. Standard content surfaces and glass action groups are separated according to [Apple's material guidance](https://developer.apple.com/design/human-interface-guidelines/materials). Desktop and Android use the shared Backdrop renderer; their screenshots do not demonstrate UIKit glass.

## Brew controls

All brew action groups use the same 20dp glyph box, including the logo with optical inset. No screen can opt into the old oversized logo. Pressure and flow remain icon-only and expose selection semantics. Free-control fixtures cover both states; tests check icon selection, slider unit/range changes and event routing without starting a brew. Flow stays unavailable on unsupported machine models.

## Boiler controls

Brew and steam controls use the supplied curved-ruler reference: a large target value, shallow arc, point marker and ticks, plus current temperature, supported range and state below. Both quick settings and full brewing settings use the same component.

Tapping or dragging snaps to machine-provided values. The large number opens the original numeric input. Boiler-off and applying states disable adjustment. Existing confirmation actions, units and BLE use cases are retained.

## Verification

The full-UI JVM catalog generates 94 images covering 47 screens/states at 1194×834pt, 2× density. The store renderer additionally covers 1024×768 landscape and phone layouts. These use production Compose components with demo fixtures and the Korean locale. `scripts/check-korean.ps1` verifies resource parity, types and placeholders.

The NativeReview XCTest scheme rotates an iPad simulator to landscape and captures home and temperature screens in day/night. `scripts/capture-native-ipad.sh` checks orientation and actual runtime `UIGlassEffect` classes. Native and JVM capture environments are labeled separately; `review/all-ui/manifest.json` identifies the current capture environment and timestamp. Native XCTest output is normalized using UIImage orientation before writing PNG pixels; it still must pass the landscape and runtime-material checks.

Local JVM checks cover profile selection/editing without starting the machine, an empty profile library, boiler input constraints, theme behavior, native frame cleanup and touch forwarding. Actual LITA hardware and physical iPad interaction remain to be verified.
