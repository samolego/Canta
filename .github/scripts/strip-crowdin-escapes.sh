#!/usr/bin/env bash
# Compose Multiplatform resources don't unescape Android's \' and \", but
# Crowdin's Android XML export always adds them. Strip them from translations.
set -euo pipefail

cd "$(dirname "$0")/../../composeApp/src/commonMain/composeResources"
sed -i -E "s/\\\\(['\"])/\\1/g" values-*/strings.xml
