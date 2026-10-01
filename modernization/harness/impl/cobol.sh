# Reference implementation of the batch steps: runs the original COBOL
# programs under GnuCOBOL. Sourced by run-scenario.sh.
#
# Step contract (shared by every implementation, e.g. a future impl/java.sh):
#   step_<name> IN_STATE_DIR OUT_DIR WORK_DIR [args...]
#   - IN_STATE_DIR / OUT_DIR hold line-sequential, fixed-length text files
#     using the app/data/ASCII record layouts (zoned decimal, EBCDIC-style
#     sign overpunch).
#   - OUT_DIR must receive every file listed for the step plus
#     <step>.sysout (program DISPLAY output) and <step>.rc (return code).

BUILD="${BUILD_DIR:-$HARNESS/build}"
export COB_LIBRARY_PATH="$BUILD"
export COB_LS_FIXED=TRUE

idx() { # idx <util> LOAD|UNLOAD <seq-file> <idx-file>
    DD_SEQFILE="$3" DD_IDXFILE="$4" "$BUILD/$1" "$2"
}

to_fixed() { # to_fixed <lrecl> <text-in> <fixed-out>
    awk -v n="$1" '{ printf "%-" n "." n "s", $0 }' "$2" > "$3"
}

from_fixed() { # from_fixed <lrecl> <fixed-in> <text-out>
    if [[ -s "$2" ]]; then fold -b -w "$1" "$2" | sed -e '$a\' > "$3"; else : > "$3"; fi
}

run_step() { # run_step <name> <out-dir> <command...>
    local name="$1" out="$2"; shift 2
    set +e
    "$@" > "$out/$name.sysout" 2>&1
    echo $? > "$out/$name.rc"
    set -e
}

# POSTTRAN / CBTRN02C
#   in : dailytran.txt cardxref.txt acctdata.txt tcatbal.txt transact.txt
#   out: acctdata.txt tcatbal.txt transact.txt dalyrejs.txt
step_posttran() {
    local in="$1" out="$2" work="$3"
    idx IDXACCT LOAD "$in/acctdata.txt" "$work/acct.idx"
    idx IDXXREF LOAD "$in/cardxref.txt" "$work/xref.idx"
    idx IDXTCAT LOAD "$in/tcatbal.txt"  "$work/tcat.idx"
    idx IDXTRAN LOAD "$in/transact.txt" "$work/tran.idx"
    to_fixed 350 "$in/dailytran.txt" "$work/dalytran.dat"

    DD_DALYTRAN="$work/dalytran.dat" DD_TRANFILE="$work/tran.idx" \
    DD_XREFFILE="$work/xref.idx" DD_DALYREJS="$work/dalyrejs.dat" \
    DD_ACCTFILE="$work/acct.idx" DD_TCATBALF="$work/tcat.idx" \
        run_step posttran "$out" cobcrun CBTRN02C

    idx IDXACCT UNLOAD "$out/acctdata.txt" "$work/acct.idx"
    idx IDXTCAT UNLOAD "$out/tcatbal.txt"  "$work/tcat.idx"
    idx IDXTRAN UNLOAD "$out/transact.txt" "$work/tran.idx"
    from_fixed 430 "$work/dalyrejs.dat" "$out/dalyrejs.txt"
}

# INTCALC / CBACT04C, PARM=<run date YYYYMMDDnn>
#   in : tcatbal.txt cardxref.txt acctdata.txt discgrp.txt
#   out: acctdata.txt systran.txt
step_intcalc() {
    local in="$1" out="$2" work="$3" parm="$4"
    idx IDXTCAT LOAD "$in/tcatbal.txt"  "$work/tcat.idx"
    idx IDXXREF LOAD "$in/cardxref.txt" "$work/xref.idx"
    idx IDXACCT LOAD "$in/acctdata.txt" "$work/acct.idx"
    idx IDXDISC LOAD "$in/discgrp.txt"  "$work/disc.idx"

    DD_TCATBALF="$work/tcat.idx" DD_XREFFILE="$work/xref.idx" \
    DD_ACCTFILE="$work/acct.idx" DD_DISCGRP="$work/disc.idx" \
    DD_TRANSACT="$work/systran.dat" \
        run_step intcalc "$out" "$BUILD/RUNPARM" CBACT04C "$parm"

    idx IDXACCT UNLOAD "$out/acctdata.txt" "$work/acct.idx"
    from_fixed 350 "$work/systran.dat" "$out/systran.txt"
}
