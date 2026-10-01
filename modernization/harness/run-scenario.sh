#!/usr/bin/env bash
# Runs a batch scenario against an implementation and writes its outputs.
# Usage: run-scenario.sh <scenario> <out-dir> [impl]   (impl defaults to cobol)
set -euo pipefail

HARNESS="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DATA="$(cd "$HARNESS/../../app/data/ASCII" && pwd)"
SCENARIO="${1:?scenario name required}"
OUT="${2:?output dir required}"
IMPL="${3:-cobol}"

# shellcheck source=impl/cobol.sh
source "$HARNESS/impl/$IMPL.sh"

# Fixed clock so CURRENT-DATE (and TRAN-PROC-TS) is reproducible.
# Other implementations must use 2022-07-18 10:11:12.00 as "now".
export COB_CURRENT_DATE="2022071810111200+0000"

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
rm -rf "$OUT"
mkdir -p "$OUT"

pad() { # pad <lrecl> <in> <out>: normalize source data to fixed-length lines
    awk -v n="$1" '{ printf "%-" n "." n "s\n", $0 }' "$2" > "$3"
}

seed_state() {
    local s="$1"
    mkdir -p "$s"
    pad 300 "$DATA/acctdata.txt"  "$s/acctdata.txt"
    pad 50  "$DATA/cardxref.txt"  "$s/cardxref.txt"
    pad 50  "$DATA/tcatbal.txt"   "$s/tcatbal.txt"
    pad 50  "$DATA/discgrp.txt"   "$s/discgrp.txt"
    pad 350 "$DATA/dailytran.txt" "$s/dailytran.txt"
    : > "$s/transact.txt"
}

carry() { # carry <from-dir> <to-dir> <files...>: copy unchanged state forward
    local from="$1" to="$2"; shift 2
    for f in "$@"; do [[ -e "$to/$f" ]] || cp "$from/$f" "$to/$f"; done
}

case "$SCENARIO" in
    daily-cycle)
        # Mirrors scripts/run_full_batch.sh: POSTTRAN then INTCALC.
        seed_state "$WORK/s0"
        mkdir -p "$WORK/w1" "$WORK/w2" "$OUT/01-posttran" "$OUT/02-intcalc"
        step_posttran "$WORK/s0" "$OUT/01-posttran" "$WORK/w1"
        mkdir -p "$WORK/s1"
        cp "$OUT/01-posttran/"*.txt "$WORK/s1/"
        carry "$WORK/s0" "$WORK/s1" cardxref.txt discgrp.txt
        step_intcalc "$WORK/s1" "$OUT/02-intcalc" "$WORK/w2" 2022071800
        ;;
    *)
        echo "unknown scenario: $SCENARIO" >&2
        exit 2
        ;;
esac

echo "Scenario $SCENARIO ($IMPL) -> $OUT"
