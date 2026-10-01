#!/usr/bin/env bash
# Compiles the pilot batch programs and harness tools with GnuCOBOL.
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP="$(cd "$HERE/../../app" && pwd)"
BUILD="${BUILD_DIR:-$HERE/build}"
PROGRAMS="${PROGRAMS:-CBTRN02C CBACT04C}"
COBFLAGS=(-std=ibm -fsign=EBCDIC)

mkdir -p "$BUILD/gen"

gen_idxutil() {
    local pgm="$1" reclen="$2" altkey="$3" layout="$4" src
    src="$(<"$HERE/tools/IDXUTIL.cbl.tpl")"
    src="${src//@PGM@/$pgm}"
    src="${src//@RECLEN@/$reclen}"
    src="${src//@ALTKEY@/$altkey}"
    src="${src//@LAYOUT@/$layout}"
    printf '%s\n' "$src" > "$BUILD/gen/$pgm.cbl"
    cobc -x "${COBFLAGS[@]}" -o "$BUILD/$pgm" "$BUILD/gen/$pgm.cbl"
}

F='           05  '
gen_idxutil IDXACCT 300 "" \
"${F}IDX-KEY                 PIC X(11).
${F}FILLER                  PIC X(289)."
gen_idxutil IDXXREF 50 "                  ALTERNATE RECORD KEY IS IDX-ALT-KEY" \
"${F}IDX-KEY                 PIC X(16).
${F}FILLER                  PIC X(9).
${F}IDX-ALT-KEY             PIC X(11).
${F}FILLER                  PIC X(14)."
gen_idxutil IDXTCAT 50 "" \
"${F}IDX-KEY                 PIC X(17).
${F}FILLER                  PIC X(33)."
gen_idxutil IDXDISC 50 "" \
"${F}IDX-KEY                 PIC X(16).
${F}FILLER                  PIC X(34)."
gen_idxutil IDXTRAN 350 "" \
"${F}IDX-KEY                 PIC X(16).
${F}FILLER                  PIC X(334)."

cobc -m "${COBFLAGS[@]}" -o "$BUILD/CEE3ABD.so" "$HERE/tools/CEE3ABD.cbl"
cobc -x "${COBFLAGS[@]}" -o "$BUILD/RUNPARM" "$HERE/tools/RUNPARM.cbl"

for p in $PROGRAMS; do
    cobc -m "${COBFLAGS[@]}" -I "$APP/cpy" -o "$BUILD/$p.so" "$APP/cbl/$p.cbl"
done

echo "Built into $BUILD: $PROGRAMS + harness tools"
