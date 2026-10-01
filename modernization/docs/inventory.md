# CardDemo inventory and migration plan

Target stack: Java 21, Spring Boot, Spring Data. Source: `app/` in this repo.

## 1. Size

| Area | Files | Lines |
| :--- | ---: | ---: |
| Core COBOL (`app/cbl`) | 31 programs | 19,496 |
| Copybooks (`app/cpy`) | 30 | 2,748 |
| BMS maps (`app/bms`) | 17 | 4,472 |
| JCL (`app/jcl`) | 38 | 2,429 |
| Assembler (`app/asm`) | 2 (`COBDATFT`, `MVSWAIT`) | - |
| Optional: authorizations IMS/DB2/MQ | 8 programs | 4,344 |
| Optional: transaction types DB2 | 3 programs | 4,037 |
| Optional: account extract VSAM/MQ | 2 programs | 1,144 |

## 2. Batch programs

| Program | JCL | Purpose | Files (DD) | Compiles on GnuCOBOL |
| :--- | :--- | :--- | :--- | :--- |
| CBTRN02C | POSTTRAN | Validate and post daily transactions; update account and category balances; write rejects | DALYTRAN (seq), TRANFILE, XREFFILE, ACCTFILE, TCATBALF (KSDS), DALYREJS (seq) | yes |
| CBACT04C | INTCALC (PARM=date) | Monthly interest per category balance; write system transactions; reset cycle totals | TCATBALF, XREFFILE (+AIX on acct id), ACCTFILE, DISCGRP (KSDS), TRANSACT (seq) | yes |
| CBTRN01C | - | Earlier draft of daily transaction validation (reads only) | DALYTRAN, CUSTFILE, XREFFILE, CARDFILE, ACCTFILE, TRANFILE | yes |
| CBTRN03C | TRANREPT | Transaction detail report with totals | TRANFILE, CARDXREF, TRANTYPE, TRANCATG, DATEPARM, TRANREPT | yes |
| CBSTM03A / CBSTM03B | CREASTMT | Account statements (text + HTML); B is the file-access subroutine | TRNXFILE, XREFFILE, CUSTFILE, ACCTFILE, STMTFILE, HTMLFILE | A: **no** (see 6), B: yes |
| CBACT01C / 02C / 03C, CBCUS01C | READACCT / READCARD / READXREF / READCUST | Dump master files | one KSDS each | yes (01C also calls `COBDATFT` asm) |
| CBEXPORT / CBIMPORT | CBEXPORT / CBIMPORT | Bulk export/import of all entities to a multi-record file | all masters | **no** (see 6) |
| COBSWAIT | WAITSTEP | Sleep (calls `MVSWAIT` asm) | - | n/a |

Nightly order (`scripts/run_full_batch.sh`): refresh data -> **POSTTRAN** -> **INTCALC** -> TRANBKP -> COMBTRAN (SORT) -> TRANIDX -> open files to CICS.

## 3. Online (CICS) programs

All screens pass state through `COCOM01Y` (COMMAREA) and move between screens with `EXEC CICS XCTL`.

| Tran | Program | Screen | VSAM files |
| :--- | :--- | :--- | :--- |
| CC00 | COSGN00C | Sign-on | USRSEC |
| CM00 | COMEN01C | User menu (options in `COMEN02Y`) | USRSEC |
| CA00 | COADM01C | Admin menu (options in `COADM02Y`) | USRSEC |
| CAVW | COACTVWC | Account view | ACCTDAT, CUSTDAT, CARDDAT, CXACAIX, CARDAIX |
| CAUP | COACTUPC | Account + customer update (4,236 LOC, largest program) | same as above |
| CCLI | COCRDLIC | Card list | CARDDAT, CARDAIX |
| CCDL | COCRDSLC | Card view | CARDDAT, CARDAIX |
| CCUP | COCRDUPC | Card update | CARDDAT, CARDAIX |
| CT00 / CT01 / CT02 | COTRN00C / 01C / 02C | Transaction list / view / add | TRANSACT (+ ACCTDAT, CCXREF, CXACAIX for add) |
| CB00 | COBIL00C | Bill payment | ACCTDAT, CXACAIX, TRANSACT |
| CR00 | CORPT00C | Report request, submits TRANREPT JCL to the internal reader | TRANSACT |
| CU00..CU03 | COUSR00C..03C | User list / add / update / delete | USRSEC |

Flow: COSGN00C -> COMEN01C (regular users) or COADM01C (admins) -> function screens -> back to the menu with PF3.

## 4. Data stores

| VSAM file | Copybook | Record length | Key | Suggested Spring Data entity |
| :--- | :--- | ---: | :--- | :--- |
| ACCTDATA | CVACT01Y | 300 | ACCT-ID 9(11) | `Account` |
| CARDDATA | CVACT02Y | 150 | CARD-NUM X(16), AIX on acct id | `Card` |
| CARDXREF | CVACT03Y | 50 | XREF-CARD-NUM X(16), AIX on acct id | `CardXref` (or a card -> account/customer relationship) |
| CUSTDATA | CVCUS01Y | 500 | CUST-ID 9(9) | `Customer` |
| TRANSACT | CVTRA05Y | 350 | TRAN-ID X(16), AIX on proc ts | `Transaction` |
| DALYTRAN | CVTRA06Y | 350 | (sequential input) | `DailyTransaction` staging / file reader |
| TCATBALF | CVTRA01Y | 50 | acct + type + category | `TransactionCategoryBalance` (composite id) |
| DISCGRP | CVTRA02Y | 50 | group + type + category | `DisclosureGroup` (composite id) |
| TRANTYPE / TRANCATG | CVTRA03Y / 04Y | 60 | type / type + category | reference tables |
| USRSEC | CSUSR01Y | 80 | USER-ID X(8) | `AppUser` (Spring Security) |

## 5. Proposed migration units (in dependency order)

| # | Unit | COBOL scope | Java target | Depends on |
| :--- | :--- | :--- | :--- | :--- |
| 0 | Domain + persistence | Entity copybooks in section 4 | JPA entities, Spring Data repositories, Flyway schema, plus a fixed-width record codec used by the parity harness | - |
| 1 | **Pilot: transaction posting** | CBTRN02C | Spring Batch job `postTransactions` | 0 |
| 2 | **Pilot: interest calculation** | CBACT04C | Spring Batch job `calculateInterest(runDate)` | 0 |
| 3 | Reports and statements | CBTRN03C, CBSTM03A/B, CBACT01-03C, CBCUS01C | Spring Batch jobs / report services | 0 |
| 4 | Export / import | CBEXPORT, CBIMPORT | Spring Batch import/export jobs | 0 (fix copybook first) |
| 5 | Auth and menus | COSGN00C, COMEN01C, COADM01C | Spring Security + menu REST API (COMMAREA becomes session/JWT claims) | 0 |
| 6 | User admin | COUSR00C-03C | `/api/users` CRUD | 5 |
| 7 | Accounts | COACTVWC, COACTUPC | `/api/accounts` (view/update with validation) | 5 |
| 8 | Cards | COCRDLIC, COCRDSLC, COCRDUPC | `/api/cards` | 5 |
| 9 | Transactions, bill pay, reports | COTRN00C-02C, COBIL00C, CORPT00C | `/api/transactions`, `/api/bills`, report job launch through `JobLauncher` | 5, 3 |
| 10 | Optional modules | DB2 tran types, IMS/DB2/MQ authorizations, VSAM/MQ extract | Spring Data JDBC/JPA, Spring JMS / messaging | 0 |

Utilities with no direct equivalent: `CSUTLDTC` (date validation through LE `CEEDAYS`) becomes `java.time`. `COBDATFT` (asm) becomes `DateTimeFormatter`. `COBSWAIT`/`MVSWAIT` are dropped. `CEE3ABD` (abend) becomes an exception plus a failed job exit status.

Why units 1 and 2 are the pilots: they are pure batch with no CICS dependency, together they cover the core money logic (validation, balances, interest), they run in the nightly cycle one after the other, and the parity harness already has golden output for both.

## 6. Findings to know before migrating

1. **CBEXPORT / CBIMPORT do not compile**: the indexed file uses `RECORD KEY IS EXPORT-SEQUENCE-NUM`, but that field comes from `COPY CVEXPORT` in WORKING-STORAGE. The FD record is a plain `PIC X(500)`, and a record key has to be a field inside the FD record. The JCL defines the export file as a KSDS with `KEYS(4 28)`, which lines up with that field, so the fix is to move the copybook under the FD.
2. **CBSTM03A does not compile**: 17 lines of `CUSTREC.cpy` contain tab characters inside the fixed-format area.
3. **CSD defines transaction `CDV1` -> program `COCRDSEC`**, and that program does not exist.
4. **`UNUSED1Y.cpy` is not referenced** by any program.
5. **`CBACT04C 1400-COMPUTE-FEES` is an empty stub** ("To be implemented"), so the Java version should keep it as a no-op unless the business owner decides otherwise.
6. **Arithmetic truncates, it does not round.** `COMPUTE WS-MONTHLY-INT = (TRAN-CAT-BAL * DIS-INT-RATE) / 1200` has no `ROUNDED`, so 1164.87 x 15.00 / 1200 = 14.560875 is stored as 14.56. In Java use `BigDecimal` with `setScale(2, RoundingMode.DOWN)`. Never use `double`.
7. **Missing disclosure group falls back to `DEFAULT`** (file status 23 then a reread). With the sample data, every type-03 category takes this path.
8. **Validation order in CBTRN02C**: unknown card (100) -> account not found (101) -> over limit (102) -> after expiration (103). 100 and 101 stop the checks. 102 and 103 are both evaluated, so when both fail, 103 replaces 102. In the sample run all 38 rejects are 102.
9. **CBACT04C never posts interest to the last account.** Account updates happen on a change of account key (control break). The end-of-file `1050-UPDATE-ACCOUNT` sits in an `ELSE` branch that can't be reached, because the `PERFORM UNTIL END-OF-FILE = 'Y'` loop exits before it runs. As a result, the last account in TCATBAL order (account 50 in the sample) gets its interest transaction written to SYSTRAN but keeps its old balance and cycle totals. The Java port does the same for parity, so fixing this is a business decision. Accounts with no category balances are not touched either.
10. **Timestamps come from `FUNCTION CURRENT-DATE`** (`TRAN-PROC-TS`, system transaction timestamps). The Java version needs an injectable `Clock` to match golden output.
11. **Signed zoned decimals use EBCDIC-style overpunch in the ASCII data** (`{` = +0, `A`-`I` = +1..9, `}` = -0, `J`-`R` = -1..9). The harness compiles with `-fsign=EBCDIC`, and the Java record codec has to do the same.
12. **New TCATBAL records inherit FILLER.** CBTRN02C builds a new category-balance record with `INITIALIZE`, which skips FILLER. The new record therefore carries the FILLER bytes of the last TCATBAL record read in the run (zeros in the sample data). The Java port reproduces this.
