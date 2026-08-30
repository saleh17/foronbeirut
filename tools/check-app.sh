#!/usr/bin/env bash
# Catches the one class of error the engine's tests structurally cannot.
#
# The engine is a separate module on purpose, and Kotlin will not smart-cast a
# public property across a module boundary. So `state.furn.items` compiles inside
# the engine and fails in the app — and since the app has no test suite and, in
# some environments, no way to compile at all, this is the cheapest guard there is.
set -euo pipefail

app="$(dirname "$0")/../app/src/main/kotlin"
nullable='furn|report|front|autoCollectAfter'

if hits=$(grep -rnE "\.($nullable)\.[A-Za-z]" "$app" 2>/dev/null); then
  echo "Cross-module smart cast — bind to a local first:"
  echo "$hits"
  exit 1
fi

echo "no cross-module smart casts"
