#!/usr/bin/env bash
# Public instrumented tests must pass; staff soft-fail. One command so
# android-emulator-runner (line-per-`sh -c`) can invoke it.
set -euo pipefail

part="${1:?milestone required, e.g. B1}"

./gradlew connectedCheck "-${part}" -public --parallel --build-cache

if ! ./gradlew connectedCheck "-${part}" -staff --parallel --build-cache; then
  echo "::warning::Staff tests failed for ${part} (non-blocking for unlock)"
fi
