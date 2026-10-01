package com.carddemo.batch.runner;

import com.carddemo.batch.posting.PostingTotalsListener;
import com.carddemo.batch.support.Sysout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Runs one batch step against master files in a directory, following the parity harness contract
 * (modernization/harness/impl/cobol.sh):
 *
 * <pre>
 * --carddemo.step=posttran|intcalc --carddemo.in=DIR --carddemo.out=DIR [--carddemo.parm=YYYYMMDDnn]
 * </pre>
 *
 * The process exit code is the COBOL RETURN-CODE: 0 OK, 4 completed with rejects, 16 abend.
 * Does nothing when {@code carddemo.step} is not set.
 */
@Component
public class BatchStepRunner implements ApplicationRunner, ExitCodeGenerator {

    static final int RC_OK = 0;
    static final int RC_WARNING = 4;
    static final int RC_ABEND = 16;

    private static final Logger log = LoggerFactory.getLogger(BatchStepRunner.class);

    private final JobLauncher jobLauncher;
    private final Job postTransactionsJob;
    private final Job interestCalculationJob;
    private final MasterFiles masterFiles;
    private final Sysout sysout;
    private final String step;
    private final String inDir;
    private final String outDir;
    private final String parm;
    private int exitCode = RC_OK;

    public BatchStepRunner(JobLauncher jobLauncher,
                           @Qualifier("postTransactionsJob") Job postTransactionsJob,
                           @Qualifier("interestCalculationJob") Job interestCalculationJob,
                           MasterFiles masterFiles, Sysout sysout,
                           @Value("${carddemo.step:}") String step,
                           @Value("${carddemo.in:}") String inDir,
                           @Value("${carddemo.out:}") String outDir,
                           @Value("${carddemo.parm:}") String parm) {
        this.jobLauncher = jobLauncher;
        this.postTransactionsJob = postTransactionsJob;
        this.interestCalculationJob = interestCalculationJob;
        this.masterFiles = masterFiles;
        this.sysout = sysout;
        this.step = step;
        this.inDir = inDir;
        this.outDir = outDir;
        this.parm = parm;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (step.isBlank()) {
            return;
        }
        Path in = Path.of(inDir);
        Path out = Path.of(outDir);
        Files.createDirectories(out);
        try {
            exitCode = switch (step) {
                case "posttran" -> postTransactions(in, out);
                case "intcalc" -> calculateInterest(in, out);
                default -> throw new IllegalArgumentException("Unknown step: " + step);
            };
        } catch (Exception e) {
            log.error("Step {} failed", step, e);
            exitCode = RC_ABEND;
        } finally {
            sysout.writeTo(out.resolve(step + ".sysout"));
        }
    }

    private int postTransactions(Path in, Path out) throws Exception {
        masterFiles.loadAccounts(in.resolve("acctdata.txt"));
        masterFiles.loadCardXrefs(in.resolve("cardxref.txt"));
        masterFiles.loadCategoryBalances(in.resolve("tcatbal.txt"));
        masterFiles.loadTransactions(in.resolve("transact.txt"));
        int rc = launch(postTransactionsJob, new JobParametersBuilder()
                .addString("dailyTransactionFile", in.resolve("dailytran.txt").toString())
                .addString("rejectFile", out.resolve("dalyrejs.txt").toString())
                .toJobParameters());
        masterFiles.unloadAccounts(out.resolve("acctdata.txt"));
        masterFiles.unloadCategoryBalances(out.resolve("tcatbal.txt"));
        masterFiles.unloadTransactions(out.resolve("transact.txt"));
        return rc;
    }

    private int calculateInterest(Path in, Path out) throws Exception {
        masterFiles.loadCategoryBalances(in.resolve("tcatbal.txt"));
        masterFiles.loadCardXrefs(in.resolve("cardxref.txt"));
        masterFiles.loadAccounts(in.resolve("acctdata.txt"));
        masterFiles.loadDisclosureGroups(in.resolve("discgrp.txt"));
        int rc = launch(interestCalculationJob, new JobParametersBuilder()
                .addString("runDate", parm)
                .addString("systemTransactionFile", out.resolve("systran.txt").toString())
                .toJobParameters());
        masterFiles.unloadAccounts(out.resolve("acctdata.txt"));
        return rc;
    }

    private int launch(Job job, JobParameters parameters) throws Exception {
        JobExecution execution = jobLauncher.run(job, parameters);
        if (execution.getStatus() != BatchStatus.COMPLETED) {
            execution.getAllFailureExceptions().forEach(e -> log.error("Job {} failed", job.getName(), e));
            return RC_ABEND;
        }
        return execution.getStepExecutions().stream()
                .map(StepExecution::getExitStatus)
                .mapToInt(BatchStepRunner::returnCode)
                .max()
                .orElse(RC_OK);
    }

    private static int returnCode(ExitStatus status) {
        if (PostingTotalsListener.COMPLETED_WITH_REJECTS.equals(status.getExitCode())) {
            return RC_WARNING;
        }
        return ExitStatus.COMPLETED.getExitCode().equals(status.getExitCode()) ? RC_OK : RC_ABEND;
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }
}
