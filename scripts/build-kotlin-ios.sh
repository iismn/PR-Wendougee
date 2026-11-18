#!/bin/bash
set -euo pipefail

if [[ "${OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED:-}" == YES ]]; then
    exit 0
fi

source "$(dirname "$0")/macos-env.sh"
cd "$WENDOUGEE_ROOT"
exec ./gradlew :shared:app:embedAndSignAppleFrameworkForXcode
