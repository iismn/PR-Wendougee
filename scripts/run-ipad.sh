#!/bin/bash
set -euo pipefail

usage() {
    cat <<'USAGE'
Wendougee LITA — USB로 연결한 iPad에서 빌드·설치·실행

  bash scripts/run-ipad.sh --check
  bash scripts/run-ipad.sh --list
  bash scripts/run-ipad.sh --device <iPad UDID> [--team <Team ID>] [--bundle-id <앱 ID>]

Xcode에서 Apple 계정으로 로그인하고, iPad를 신뢰·페어링한 뒤 개발자 모드를 켜세요.
--list에 표시되는 iPad UDID를 사용합니다. 서명 팀은 Xcode 또는
iosApp/Configuration/Local.xcconfig에 저장해 두면 매번 지정할 필요가 없습니다.
USAGE
}

mode=check
device_id=
signing_team=
bundle_id=
while [[ $# -gt 0 ]]; do
    case "$1" in
        --help|-h) usage; exit 0 ;;
        --check) mode=check; shift ;;
        --list) mode=list; shift ;;
        --device|--team|--bundle-id)
            [[ $# -ge 2 && -n "$2" && "$2" != --* ]] || { usage >&2; exit 2; }
            case "$1" in
                --device) device_id="$2"; mode=run ;;
                --team) signing_team="$2" ;;
                --bundle-id) bundle_id="$2" ;;
            esac
            shift 2
            ;;
        *) usage >&2; exit 2 ;;
    esac
done

[[ "$(uname -s)" == Darwin ]] || { echo "이 스크립트는 Mac에서 실행하세요." >&2; exit 1; }
source "$(dirname "$0")/macos-env.sh"
cd "$WENDOUGEE_ROOT"

if ! xcodebuild -version; then
    echo "Xcode를 설치하고 한 번 실행해 초기 설정을 완료해 주세요." >&2
    echo "설치: https://apps.apple.com/app/xcode/id497799835" >&2
    exit 1
fi
if ! xcrun --sdk iphoneos --show-sdk-path; then
    echo "Xcode > Settings > Components에서 iOS 플랫폼을 설치해 주세요." >&2
    exit 1
fi

case "$mode" in
    check)
        java -version
        if [[ -z "${ANDROID_HOME:-}" && ! -f local.properties ]]; then
            echo "Android SDK도 필요합니다. docs/MAC_HANDOFF.md의 SDK 설정을 확인하세요." >&2
            exit 1
        fi
        echo "빌드 도구 확인 완료. --list로 iPad UDID를 확인하세요."
        exit 0
        ;;
    list)
        exec xcrun xctrace list devices
        ;;
esac

mkdir -p .tooling
device_info="$WENDOUGEE_ROOT/.tooling/ipad-device-check.json"
xcrun devicectl device info details --device "$device_id" --timeout 30 --json-output "$device_info" > /dev/null
pairing_state="$(plutil -extract result.connectionProperties.pairingState raw -o - "$device_info" 2>/dev/null || true)"
if [[ "$pairing_state" == unpaired ]]; then
    if ! xcrun devicectl manage pair --device "$device_id"; then
        echo "Finder에서 iPad를 선택해 신뢰를 누르고, iPad 화면에서도 승인한 뒤 다시 실행하세요." >&2
        exit 1
    fi
    xcrun devicectl device info details --device "$device_id" --timeout 30 --json-output "$device_info" > /dev/null
fi
developer_mode="$(plutil -extract result.deviceProperties.developerModeStatus raw -o - "$device_info" 2>/dev/null || true)"
if [[ "$developer_mode" == disabled ]]; then
    echo "iPad의 설정 → 개인정보 보호 및 보안 → 개발자 모드를 켜고 재시동한 뒤 다시 실행하세요." >&2
    exit 1
fi

overrides=("CODE_SIGN_STYLE=Automatic")
[[ -z "$signing_team" ]] || overrides+=("DEVELOPMENT_TEAM=$signing_team")
[[ -z "$bundle_id" ]] || overrides+=("PRODUCT_BUNDLE_IDENTIFIER=$bundle_id")
if ! xcodebuild \
    -quiet \
    -project iosApp/iosApp.xcodeproj \
    -scheme iosApp \
    -configuration Debug \
    -sdk iphoneos \
    -destination "id=$device_id" \
    -destination-timeout 120 \
    -derivedDataPath "$WENDOUGEE_ROOT/build/ipad" \
    -allowProvisioningUpdates \
    -allowProvisioningDeviceRegistration \
    "${overrides[@]}" build 2>&1 | tee .tooling/ipad-build.log; then
    echo "빌드가 중단되었습니다. 로그: $WENDOUGEE_ROOT/.tooling/ipad-build.log" >&2
    exit 1
fi

app_path="$WENDOUGEE_ROOT/build/ipad/Build/Products/Debug-iphoneos/GeeFlow.app"
[[ -d "$app_path" ]] || { echo "빌드된 앱을 찾을 수 없습니다: $app_path" >&2; exit 1; }
installed_bundle_id="$(/usr/libexec/PlistBuddy -c 'Print CFBundleIdentifier' "$app_path/Info.plist")"
codesign --verify --strict "$app_path"
xcrun devicectl device install app --device "$device_id" "$app_path"
if ! xcrun devicectl device process launch --device "$device_id" "$installed_bundle_id"; then
    echo "앱 설치는 완료됐으나 실행되지 않았습니다. iPad에서 Wendougee LITA를 열어 표시되는 안내를 확인하세요." >&2
    echo "'신뢰하지 않는 개발자'가 표시되면 설정 → 일반 → VPN 및 기기 관리에서 본인의 개발자 서명을 확인하세요." >&2
    exit 1
fi
echo "iPad에서 Wendougee LITA를 실행했습니다."
