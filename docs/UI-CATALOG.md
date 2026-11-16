# Wendougee LITA UI review

The reference home is `profile-home-day-icons-v2.png` and `profile-home-night-icons-v2.png`. The home layout follows those references; the current circular Wendougee mark replaces their former drop logo. Other screens use the same Pretendard type roles, action-icon sizes, surfaces and spacing; editors share its three-column workspace. Quick temperature control opens as a popup over home.

## Open the gallery

```sh
./gradlew :shared:app:generateAllScreens
node scripts/preview-all-ui.cjs
```

Open `http://127.0.0.1:4182`. Select a screen, day/night, **홈과 비교** (compare with home), **낮·밤 비교**, or **전체 보기**. PNG links open the full-resolution capture. The viewer updates after rendering completes.

These are production Compose components with local demo fixtures at 1194×834pt, density 2, Korean locale. The gallery does not control the machine. It is not a UIKit Liquid Glass capture or an interactive port of the entire application.

## Coverage

48 states, each captured in day and night:

| Area | Screens and states |
| --- | --- |
| Brewing | Profile home, profile editor, step editor, quick temperature popup, temperature applying, free pressure control, free flow control, pressure/flow charts, brew history |
| Machine | Quick cleaning in progress, boiler settings, connectivity, machine settings menu, maintenance settings |
| User | User list, about, appearance, brewing preferences, user settings menu, profile settings, add user, open-source licenses, screensaver |
| Setup | Add device, device list, welcome, welcome profile form, device discovered |
| Dialogs | Bluetooth permission, cleaning reminder, profile details, finish target, grinder settings, step name/value, user name/delete/photo removal, appearance mode/theme/palette/custom color, screensaver timeout, temperature unit, profile restore, device model/name/warning |

Step forms share one continuous parent background. Brew action icons use a single size across all screens. The screensaver uses a smaller glowing dot clock, with tiny Pretendard AM/PM indicators on the left and no date. The welcome screen centers the official circular mark, Wendougee title and profile-creation prompt; its form opens in the shared popup.

The catalog covers the main custom screens and selected dialogs, rather than every possible data or error state. Operating-system camera, photo library, permissions and language settings must be checked on their target platform.

## Implementation

| Source | Purpose |
| --- | --- |
| `shared/core/ui/.../components/CoffeeWorkspace.kt` | Home-derived spacing, typography, metrics and workspace panels |
| `shared/feature/device/dashboard/.../main/ProfileHome.kt` | Approved home composition |
| `shared/feature/device/dashboard/.../profileeditor/` | Production profile and step editors |
| `shared/feature/device/dashboard/.../quicksettings/` | Production brew/steam controls and event handling |
| `shared/app/src/jvmMain/kotlin/app/geeflow/app/review/ReviewScenes.kt` | Catalog and fixture entry points |
| `shared/app/src/jvmMain/kotlin/app/geeflow/app/review/SettingsPair.kt` | Actual list/detail navigation used in settings captures |
| `shared/core/ui/.../components/DialogCaptureHost.kt` | Reuses the production dialog body in the JVM capture window |
| `iosApp/iosApp/NativeGlassViewController.swift` | Native UIKit material hosting |

Actual app dialogs continue to use their normal dialog window. The capture host is enabled only by review entry points. Public preview wrappers do not perform BLE or account writes.

Native review runs in Xcode through `NativeReview`; Windows cannot execute that SDK. See `MAC_HANDOFF.md` for local Mac instructions. Capture environment and generation time are in `review/all-ui/manifest.json`.
