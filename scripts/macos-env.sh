#!/bin/bash
# Source this file from the Xcode build phase or the local command line.
WENDOUGEE_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]:-$0}")/.." && pwd)"

if [[ -z "${JAVA_HOME:-}" ]] || ! "$JAVA_HOME/bin/java" -version 2>&1 | /usr/bin/grep -q 'version "21[."]'; then
    if [[ -x "$WENDOUGEE_ROOT/.tooling/jdk-21/Contents/Home/bin/java" ]]; then
        export JAVA_HOME="$WENDOUGEE_ROOT/.tooling/jdk-21/Contents/Home"
    else
        JAVA_HOME="$(/usr/libexec/java_home -v 21 2>/dev/null)" || {
            echo "JDK 21이 필요합니다. JDK 21을 설치하거나 JAVA_HOME을 지정해 주세요." >&2
            return 1
        }
        export JAVA_HOME
    fi
fi
export PATH="$JAVA_HOME/bin:$PATH"

# Keep this checkout's downloaded tools and build caches together.
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-$WENDOUGEE_ROOT/.tooling/gradle}"
export KONAN_DATA_DIR="${KONAN_DATA_DIR:-$WENDOUGEE_ROOT/.tooling/konan}"

if [[ -z "${ANDROID_HOME:-}" ]]; then
    if [[ -d "$WENDOUGEE_ROOT/.tooling/android-sdk" ]]; then
        export ANDROID_HOME="$WENDOUGEE_ROOT/.tooling/android-sdk"
    elif [[ -d "$HOME/Library/Android/sdk" ]]; then
        export ANDROID_HOME="$HOME/Library/Android/sdk"
    fi
fi

# A GUI launch of Xcode does not read the user's shell configuration.
if [[ -z "${DEVELOPER_DIR:-}" && -d /Applications/Xcode.app/Contents/Developer ]]; then
    export DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer
fi
