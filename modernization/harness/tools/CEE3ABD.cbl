       IDENTIFICATION DIVISION.
       PROGRAM-ID. CEE3ABD.
      * Local stand-in for the LE abend service: report and terminate.
       DATA DIVISION.
       LINKAGE SECTION.
       01  ABCODE                  PIC S9(9) BINARY.
       01  TIMING                  PIC S9(9) BINARY.
       PROCEDURE DIVISION USING ABCODE TIMING.
           DISPLAY 'CEE3ABD: ABEND U' ABCODE UPON SYSERR
           STOP RUN RETURNING 16.
