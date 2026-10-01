       IDENTIFICATION DIVISION.
       PROGRAM-ID. RUNPARM.
      * Emulates EXEC PGM=xxx,PARM='...': calls the program with an
      * IBM-style PARM area (halfword length + text).
      * Usage: RUNPARM <program> [parm]
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01  WS-PGM                  PIC X(8).
       01  WS-PARM-AREA.
           05  WS-PARM-LEN         PIC S9(4) COMP.
           05  WS-PARM-TEXT        PIC X(100).
       PROCEDURE DIVISION.
           ACCEPT WS-PGM FROM ARGUMENT-VALUE
           MOVE SPACES TO WS-PARM-TEXT
           ACCEPT WS-PARM-TEXT FROM ARGUMENT-VALUE
           MOVE FUNCTION LENGTH(FUNCTION TRIM(WS-PARM-TEXT TRAILING))
             TO WS-PARM-LEN
           IF WS-PARM-TEXT = SPACES
               MOVE 0 TO WS-PARM-LEN
           END-IF
           CALL WS-PGM USING WS-PARM-AREA
           STOP RUN.
