#!/usr/bin/env bash
# Compares an implementation's scenario output against the golden master.
# Usage: compare.sh <scenario> <actual-dir>
set -euo pipefail

HARNESS="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
GOLDEN="$HARNESS/../golden/${1:?scenario name required}"
ACTUAL="${2:?actual dir required}"

if diff -ru "$GOLDEN" "$ACTUAL"; then
    echo "PARITY OK: $1"
else
    echo "PARITY FAILED: $1" >&2
    exit 1
fi
