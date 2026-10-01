package com.carddemo.batch.interest;

import com.carddemo.batch.layout.TransactionLayout;
import com.carddemo.batch.support.ProgramBanner;
import com.carddemo.batch.support.Sysout;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** INTCALC job (COBOL CBACT04C). Job parameters: {@code runDate}, {@code systemTransactionFile}. */
@Configuration
public class InterestCalculationJobConfig {

    public static final String JOB_NAME = "interestCalculationJob";

    @Bean
    public Job interestCalculationJob(JobRepository jobRepository, Step interestCalculationStep, Sysout sysout) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .listener(new ProgramBanner("CBACT04C", sysout))
                .start(interestCalculationStep)
                .build();
    }

    @Bean
    public Step interestCalculationStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                                        Tasklet interestCalculationTasklet) {
        return new StepBuilder("interestCalculationStep", jobRepository)
                .tasklet(interestCalculationTasklet, transactionManager)
                .build();
    }

    @Bean
    @StepScope
    public Tasklet interestCalculationTasklet(InterestCalculationService service,
                                              @Value("#{jobParameters['runDate']}") String runDate,
                                              @Value("#{jobParameters['systemTransactionFile']}") String output) {
        return (contribution, chunkContext) -> {
            try (BufferedWriter writer = Files.newBufferedWriter(Path.of(output), StandardCharsets.ISO_8859_1)) {
                service.calculate(runDate, transaction -> {
                    try {
                        writer.write(TransactionLayout.format(transaction));
                        writer.write('\n');
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
            }
            return RepeatStatus.FINISHED;
        };
    }
}
