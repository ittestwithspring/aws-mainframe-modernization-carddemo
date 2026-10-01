package com.carddemo.batch.posting;

import com.carddemo.batch.support.Sysout;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;

/** Prints CBTRN02C's totals and flags runs with rejects (COBOL RETURN-CODE 4). */
public class PostingTotalsListener implements StepExecutionListener {

    public static final String COMPLETED_WITH_REJECTS = "COMPLETED_WITH_REJECTS";

    private final Sysout sysout;

    public PostingTotalsListener(Sysout sysout) {
        this.sysout = sysout;
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        if (stepExecution.getStatus() != BatchStatus.COMPLETED) {
            return stepExecution.getExitStatus();
        }
        long rejected = stepExecution.getWriteCount();
        sysout.display(String.format("TRANSACTIONS PROCESSED :%09d", stepExecution.getReadCount()));
        sysout.display(String.format("TRANSACTIONS REJECTED  :%09d", rejected));
        return rejected > 0 ? new ExitStatus(COMPLETED_WITH_REJECTS) : stepExecution.getExitStatus();
    }
}
