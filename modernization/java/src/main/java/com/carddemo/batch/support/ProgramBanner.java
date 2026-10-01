package com.carddemo.batch.support;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;

/** Writes the START/END OF EXECUTION lines the COBOL programs DISPLAY. */
public class ProgramBanner implements JobExecutionListener {

    private final String program;
    private final Sysout sysout;

    public ProgramBanner(String program, Sysout sysout) {
        this.program = program;
        this.sysout = sysout;
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        sysout.display("START OF EXECUTION OF PROGRAM " + program);
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            sysout.display("END OF EXECUTION OF PROGRAM " + program);
        }
    }
}
