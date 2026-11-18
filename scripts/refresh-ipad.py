#!/usr/bin/env python3
"""Refresh the approved iPad build before its Personal Team profile expires.

Run with this Mac's normal logged-in user. Apple credentials stay in Xcode/Keychain.
Never uninstalls the app, launches it, or stops an active espresso session.
"""
import argparse
import datetime as dt
import fcntl
import hashlib
import json
import os
from pathlib import Path
import plistlib
import shutil
import subprocess
import sys

ROOT = Path(__file__).resolve().parent.parent
WORK = ROOT / '.tooling/ipad-renewal'
STATE = WORK / 'state.json'
DEFAULT_APP = ROOT / 'build/ipad/Build/Products/Debug-iphoneos/GeeFlow.app'
UTC = dt.timezone.utc


def emit(status, **values):
    print(json.dumps(dict(status=status, **values), ensure_ascii=False))


def now():
    return dt.datetime.now(UTC)


def iso(value):
    return value.replace(tzinfo=UTC).isoformat()


def profile(app):
    data = (app / 'embedded.mobileprovision').read_bytes()
    start = data.index(b'<?xml')
    # Metadata only; codesign separately verifies the installed bundle.
    return plistlib.loads(data[start:data.index(b'</plist>', start) + len(b'</plist>')])


def fingerprint():
    files = subprocess.check_output(['git', 'ls-files', '-z', '--cached', '--others', '--exclude-standard'], cwd=ROOT).split(b'\0')
    selected = {'iosApp/Configuration/Local.xcconfig'}
    for raw in files:
        name = raw.decode()
        if not name or '/build/' in name or (name.startswith('shared/') and '/src/' in name and not any('/' + s + '/' in name for s in ('commonMain', 'iosMain', 'nativeMain', 'appleMain'))):
            continue
        if name.startswith(('shared/', 'iosApp/', 'gradle/', 'build-logic/')) or name in ('build.gradle.kts', 'settings.gradle.kts', 'gradle.properties', 'scripts/build-kotlin-ios.sh', 'scripts/macos-env.sh'):
            selected.add(name)
    result = hashlib.sha256()
    for name in sorted(selected):
        path = ROOT / name
        if not path.is_file():
            continue
        data = path.read_bytes()
        if name.endswith('.pbxproj'):
            data = json.dumps(json.loads(subprocess.check_output(['plutil', '-convert', 'json', '-o', '-', str(path)])), sort_keys=True).encode()
        result.update(name.encode() + b'\0' + data + b'\0')
    return result.hexdigest()


def save(state):
    temp = STATE.with_suffix('.tmp')
    temp.write_text(json.dumps(state, ensure_ascii=False, indent=2) + '\n')
    temp.replace(STATE)


def command(args, *, logfile=None, timeout=60):
    if logfile:
        with logfile.open('w') as output:
            subprocess.run(args, cwd=ROOT, stdout=output, stderr=subprocess.STDOUT, check=True, timeout=timeout)
    else:
        subprocess.run(args, cwd=ROOT, stdout=subprocess.DEVNULL, stderr=subprocess.PIPE, check=True, timeout=timeout)


def device_json(state, noun):
    path = WORK / f'device-{noun}.json'
    command(['xcrun', 'devicectl', 'device', 'info', noun, '--device', state['device'], '--timeout', '20', '--json-output', str(path)], timeout=35)
    return json.loads(path.read_text())['result']


def in_use(state):
    return any('/GeeFlow.app/' in item.get('executable', '') for item in device_json(state, 'processes').get('runningProcesses', []))


def validate(app, state):
    with (app / 'Info.plist').open('rb') as source:
        info = plistlib.load(source)
    p = profile(app)
    if info['CFBundleIdentifier'] != state['bundleId'] or p['Entitlements']['application-identifier'] != state['team'] + '.' + state['bundleId'] or state['device'] not in p.get('ProvisionedDevices', []):
        raise RuntimeError('Signing identity, bundle ID, or target device does not match the approved app')
    command(['codesign', '--verify', '--deep', '--strict', str(app)])
    return p


def approve(args):
    app = Path(args.app).resolve()
    p = profile(app)
    with (app / 'Info.plist').open('rb') as source:
        bundle = plistlib.load(source)['CFBundleIdentifier']
    state = dict(device=args.device, team=p['TeamIdentifier'][0], bundleId=bundle, sourceFingerprint=fingerprint())
    p = validate(app, state)
    state.update(expiration=iso(p['ExpirationDate']), profileUUID=p['UUID'], approvedAt=now().isoformat(), approvedApp=str(app))
    save(state)
    emit('approved', expiration=state['expiration'], device=state['device'])


def renew(state, force):
    expiration = dt.datetime.fromisoformat(state['expiration'])
    hours = (expiration - now()).total_seconds() / 3600
    if not force and hours > 72:
        emit('healthy', expiration=state['expiration'], hoursRemaining=round(hours, 1))
        return
    if fingerprint() != state['sourceFingerprint']:
        emit('source_changed', expiration=state['expiration'], reason='Review and test source changes before approving the next build')
        return
    try:
        details = device_json(state, 'details')
        if details['connectionProperties']['pairingState'] != 'paired' or details['deviceProperties']['developerModeStatus'] != 'enabled':
            raise RuntimeError('iPad pairing or Developer Mode needs attention')
        if in_use(state):
            emit('app_in_use', expiration=state['expiration'], hoursRemaining=round(hours, 1))
            return
    except (subprocess.SubprocessError, KeyError, RuntimeError) as error:
        emit('device_unavailable', expiration=state['expiration'], hoursRemaining=round(hours, 1), reason=str(error))
        return

    # Move only this app's cached profiles aside. Keep recoverable backups. Xcode
    # requests a fresh profile from Apple; never change certificates or accounts.
    moved = []
    backup = WORK / ('profiles-' + now().strftime('%Y%m%dT%H%M%SZ'))
    backup.mkdir(exist_ok=True)
    success = False
    try:
        for directory in (Path.home() / 'Library/Developer/Xcode/UserData/Provisioning Profiles', Path.home() / 'Library/MobileDevice/Provisioning Profiles'):
            for path in directory.glob('*.mobileprovision'):
                try:
                    p = profile_file(path)
                except (ValueError, plistlib.InvalidFileException):
                    continue
                if p.get('Entitlements', {}).get('application-identifier') == state['team'] + '.' + state['bundleId'] and state['device'] in p.get('ProvisionedDevices', []):
                    target = backup / (hashlib.sha256(str(path).encode()).hexdigest()[:8] + '-' + path.name)
                    shutil.move(str(path), target)
                    moved.append((path, target))
        build_log = WORK / 'build.log'
        derived = ROOT / 'build/ipad-renewal'
        build = ['xcodebuild', '-quiet', '-project', 'iosApp/iosApp.xcodeproj', '-scheme', 'iosApp', '-configuration', 'Debug', '-sdk', 'iphoneos', '-destination', 'id=' + state['device'], '-destination-timeout', '60', '-derivedDataPath', str(derived), '-allowProvisioningUpdates', '-allowProvisioningDeviceRegistration', 'CODE_SIGN_STYLE=Automatic', 'DEVELOPMENT_TEAM=' + state['team'], 'PRODUCT_BUNDLE_IDENTIFIER=' + state['bundleId'], 'build']
        # The script path is positional, never interpolated as shell code.
        command(['/bin/bash', '-c', 'source "$1"; shift; exec "$@"', 'refresh-ipad', str(ROOT / 'scripts/macos-env.sh'), *build], logfile=build_log, timeout=1800)
        app = derived / 'Build/Products/Debug-iphoneos/GeeFlow.app'
        p = validate(app, state)
        if p['ExpirationDate'].replace(tzinfo=UTC) <= expiration:
            emit('profile_not_extended', expiration=state['expiration'], log=str(build_log))
            return
        if fingerprint() != state['sourceFingerprint']:
            emit('source_changed', expiration=state['expiration'])
            return
        if in_use(state):
            emit('app_in_use', expiration=state['expiration'])
            return
        command(['xcrun', 'devicectl', 'device', 'install', 'app', '--device', state['device'], '--timeout', '120', str(app)], logfile=WORK / 'install.log', timeout=150)
        state.update(expiration=iso(p['ExpirationDate']), profileUUID=p['UUID'], lastRenewedAt=now().isoformat(), approvedApp=str(app))
        save(state)
        success = True
        emit('renewed', expiration=state['expiration'], log=str(build_log))
    finally:
        if not success:
            for original, copy in moved:
                if not original.exists() and copy.exists():
                    shutil.move(str(copy), original)


def profile_file(path):
    data = path.read_bytes()
    start = data.index(b'<?xml')
    return plistlib.loads(data[start:data.index(b'</plist>', start) + 8])


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument('--check', action='store_true')
    group.add_argument('--approve-current-build', action='store_true')
    group.add_argument('--renew', action='store_true')
    parser.add_argument('--force', action='store_true', help='Attempt renewal now, still deferring if the app is running')
    parser.add_argument('--device')
    parser.add_argument('--app', default=str(DEFAULT_APP))
    args = parser.parse_args()
    if args.approve_current_build and not args.device:
        parser.error('--approve-current-build requires --device')
    WORK.mkdir(parents=True, exist_ok=True)
    with (WORK / 'lock').open('w') as lock:
        try:
            fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except BlockingIOError:
            emit('already_running')
            return
        if args.approve_current_build:
            approve(args)
        else:
            state = json.loads(STATE.read_text())
            if args.check:
                emit('status', **state, hoursRemaining=round((dt.datetime.fromisoformat(state['expiration']) - now()).total_seconds() / 3600, 1), sourceUnchanged=fingerprint() == state['sourceFingerprint'])
            else:
                renew(state, args.force)


if __name__ == '__main__':
    try:
        main()
    except Exception as error:
        emit('failed', reason=str(error), logs=str(WORK))
        sys.exit(1)
