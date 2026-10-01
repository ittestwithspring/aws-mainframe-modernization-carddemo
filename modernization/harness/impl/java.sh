# Java 21 / Spring Boot implementation of the batch steps (modernization/java).
# Sourced by run-scenario.sh; same step contract as impl/cobol.sh.
# Build first: mvn -f modernization/java/pom.xml package

JAR="${CARDDEMO_JAR:-$HARNESS/../java/target/carddemo-batch.jar}"
[[ -f "$JAR" ]] || { echo "missing $JAR; run: mvn -f modernization/java/pom.xml package" >&2; exit 2; }

run_java() { # run_java <step> <in> <out> <work> [parm]
    local name="$1" in="$2" out="$3" work="$4" parm="${5:-}"
    set +e
    java -jar "$JAR" --carddemo.step="$name" --carddemo.in="$in" --carddemo.out="$out" \
        --carddemo.parm="$parm" --carddemo.clock.fixed=2022-07-18T10:11:12Z > "$work/$name.log" 2>&1
    echo $? > "$out/$name.rc"
    set -e
}

step_posttran() { run_java posttran "$1" "$2" "$3"; }

step_intcalc() { run_java intcalc "$1" "$2" "$3" "$4"; }
