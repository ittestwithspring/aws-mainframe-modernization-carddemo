# CardDemo modernization: COBOL to Java 21 / Spring Boot

- `docs/inventory.md`: program/data inventory, migration units, pilot choice, gotchas.
- `harness/`: runs the original COBOL batch locally with GnuCOBOL and checks any implementation against golden output.
- `golden/`: expected output recorded from the COBOL run.
- `java/`: Java 21 / Spring Boot / Spring Batch / Spring Data JPA port of the pilot jobs (POSTTRAN, INTCALC).

## Prerequisites

```bash
sudo apt-get install -y gnucobol3   # GnuCOBOL 3.1.2, BDB indexed files
sudo apt-get install -y openjdk-21-jdk-headless maven   # Java port
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

### Java implementation

`impl/java.sh` runs the Spring Boot jar from `java/` with the same step contract. Each step starts a fresh in-memory H2 database, loads the input files through Spring Data JPA, runs the Spring Batch job, then unloads the results back to fixed-width text.

```bash
mvn -f ../java/pom.xml package     # build + unit tests + DailyCycleParityTest
./run-scenario.sh daily-cycle /tmp/java-out java && ./compare.sh daily-cycle /tmp/java-out
```

Parity holds when every file matches byte for byte. The Java port currently matches all of them, including `sysout` and return codes.

| COBOL | Java |
|---|---|
| CBTRN02C (POSTTRAN) | `postTransactionsJob`: chunk step, `TransactionPostingService` |
| CBACT04C (INTCALC) | `interestCalculationJob`: tasklet step, `InterestCalculationService` |
| VSAM KSDS + copybook | JPA entity + `JpaRepository` (`domain/`), fixed-width codec (`layout/`) |
| `PIC S9(n)V99` zoned decimal | `BigDecimal`, `Zoned` (overpunch codec, COBOL truncation) |
| `FUNCTION CURRENT-DATE` | `BatchClock` (`carddemo.clock.fixed` pins it) |
| `RETURN-CODE` 0 / 4 / abend | process exit code 0 / 4 / 16 |

Known COBOL quirks are reproduced on purpose for parity (see findings 8, 9 and 12 in `docs/inventory.md`). The biggest one is that INTCALC never updates the last account.

### Regenerating golden output

Only do this when the COBOL source or sample data changes on purpose:

```bash
./run-scenario.sh daily-cycle ../golden/daily-cycle
```
