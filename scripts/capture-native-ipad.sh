#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$ROOT/build/native-review"
APP="$ROOT/build/ios-review/Build/Products/Debug-iphonesimulator/GeeFlow.app"
mkdir -p "$OUT"
RUNTIME=$(xcrun simctl list runtimes -j | python3 -c 'import sys,json; r=[r for r in json.load(sys.stdin)["runtimes"] if r.get("isAvailable") and r["name"].startswith("iOS") and int(r["version"].split(".")[0])>=26]; assert r,"iOS 26+ runtime is required"; print(sorted(r,key=lambda r:tuple(map(int,r["version"].split("."))))[-1]["identifier"])')
TYPE=$(xcrun simctl list devicetypes -j | python3 -c 'import sys,json; d=[d for d in json.load(sys.stdin)["devicetypes"] if "iPad Pro" in d["name"] and "11-inch" in d["name"]]; assert d; print(d[-1]["identifier"])')
UDID=$(xcrun simctl create "GeeFlow Native Review" "$TYPE" "$RUNTIME")
trap 'xcrun simctl shutdown "$UDID" >/dev/null 2>&1 || true' EXIT
xcrun simctl boot "$UDID"
xcrun simctl bootstatus "$UDID" -b
BUNDLE=$(/usr/libexec/PlistBuddy -c 'Print CFBundleIdentifier' "$APP/Info.plist")
xcrun simctl install "$UDID" "$APP"
xcrun simctl status_bar "$UDID" override --time '9:41' --dataNetwork wifi --wifiMode active --wifiBars 3 --batteryState charged --batteryLevel 100
for scene in dashboard temperature; do
  for theme in day night; do
    xcrun simctl terminate "$UDID" "$BUNDLE" >/dev/null 2>&1 || true
    xcrun simctl launch "$UDID" "$BUNDLE" --design-review "$scene" "--$theme" -AppleLanguages '(ko)' -AppleLocale 'ko_KR'
    sleep 12
    xcrun simctl io "$UDID" screenshot "$OUT/ipad-$scene-$theme.png"
    CONTAINER=$(xcrun simctl get_app_container "$UDID" "$BUNDLE" data)
    cp "$CONTAINER/Documents/native-materials.json" "$OUT/ipad-$scene-$theme.json"
  done
done
python3 - "$OUT" <<'PY'
import json,pathlib,sys,struct
out=pathlib.Path(sys.argv[1])
for scene in ('dashboard','temperature'):
    for theme in ('day','night'):
        stem=out/f'ipad-{scene}-{theme}'
        data=json.loads(stem.with_suffix('.json').read_text())
        assert int(data['os'].split('.')[0])>=26, data
        assert data['phase']==(3 if theme=='night' else 1),data
        assert data['scene']==scene,data
        if scene=='dashboard':
            assert len(data['materials'])>=3,data
            assert all('UIGlassEffect' in m['effect'] for m in data['materials']),data
            assert all(m['width']>0 and m['height']>0 for m in data['materials']),data
        with stem.with_suffix('.png').open('rb') as f:
            f.seek(16); width,height=struct.unpack('>II',f.read(8))
        assert width>height, f'Expected landscape screenshot: {stem} {width}x{height}'
        print(f'{stem.name}: {width}x{height}, iPadOS {data["os"]}, native glass objects={len(data["materials"])}')
PY
git rev-parse HEAD > "$OUT/source-commit.txt"
