# CardDemo modernization: COBOL to Java 21 / Spring Boot

- `docs/inventory.md`: program/data inventory, migration units, pilot choice, gotchas.
- `harness/`: runs the original COBOL batch locally with GnuCOBOL and checks any implementation against golden output.
- `golden/`: expected output recorded from the COBOL run.

## Prerequisites

```bash
sudo apt-get install -y gnucobol3   # GnuCOBOL 3.1.2, BDB indexed files
```

## Run

```bash
cd modernization/harness
./build.sh                                    # compile CBTRN02C, CBACT04C + tools
./run-scenario.sh daily-cycle /tmp/out        # run the batch cycle (default impl: cobol)
./compare.sh daily-cycle /tmp/out             # diff against golden/daily-cycle
```

## How the harness works

The `daily-cycle` scenario follows the nightly flow: **POSTTRAN (CBTRN02C) -> INTCALC (CBACT04C, PARM=2022071800)**. Its input is the sample data in `app/data/ASCII`.

- Batch state between steps is stored as line-sequential, fixed-length text files that use the copybook layouts. This is the same format as `app/data/ASCII`.
- `impl/cobol.sh` loads that state into GnuCOBOL indexed files (stand-ins for VSAM KSDS), runs the program, then unloads the results back to text. `RUNPARM` passes a JCL-style `PARM`, and `CEE3ABD` stands in for the LE abend service.
- The clock is fixed at `2022-07-18 10:11:12.00` (`COB_CURRENT_DATE`), so every run produces the same bytes.
- Each step writes its data files, `<step>.sysout` (DISPLAY output) and `<step>.rc` (return code). CBTRN02C returns 4 when there are rejects.

### Plugging in the Java implementation

Add `impl/java.sh` that defines `step_posttran IN OUT WORK` and `step_intcalc IN OUT WORK PARM`. The functions read and write the same text files (documented in `impl/cobol.sh`), so the Java job can run against an embedded DB and import/export fixed-width records. Then run:

```bash
./run-scenario.sh daily-cycle /tmp/java-out java && ./compare.sh daily-cycle /tmp/java-out
```

Parity holds when every data file matches byte for byte. Matching `sysout` is optional, depending on how faithful the logging needs to be.

### Regenerating golden output

Only do this when the COBOL source or sample data changes on purpose:

```bash
./run-scenario.sh daily-cycle ../golden/daily-cycle
```
