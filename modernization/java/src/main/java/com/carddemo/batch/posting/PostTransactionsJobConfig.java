package com.carddemo.batch.posting;

import com.carddemo.batch.layout.Fields;
import com.carddemo.batch.layout.TransactionLayout;
import com.carddemo.batch.support.ProgramBanner;
import com.carddemo.batch.support.Sysout;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

/** POSTTRAN job (COBOL CBTRN02C). Job parameters: {@code dailyTransactionFile}, {@code rejectFile}. */
@Configuration
public class PostTransactionsJobConfig {

    public static final String JOB_NAME = "postTransactionsJob";

    @Bean
    public Job postTransactionsJob(JobRepository jobRepository, Step postTransactionsStep, Sysout sysout) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .listener(new ProgramBanner("CBTRN02C", sysout))
                .start(postTransactionsStep)
                .build();
    }

    @Bean
    public Step postTransactionsStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                                     FlatFileItemReader<DailyTransaction> dailyTransactionReader,
                                     TransactionPostingProcessor transactionPostingProcessor,
                                     FlatFileItemWriter<RejectRecord> rejectWriter, Sysout sysout) {
        return new StepBuilder("postTransactionsStep", jobRepository)
                .<DailyTransaction, RejectRecord>chunk(100, transactionManager)
                .reader(dailyTransactionReader)
                .processor(transactionPostingProcessor)
                .writer(rejectWriter)
                .listener(new PostingTotalsListener(sysout))
                .build();
    }

    @Bean
    @StepScope
    public FlatFileItemReader<DailyTransaction> dailyTransactionReader(
            @Value("#{jobParameters['dailyTransactionFile']}") String path) {
        return new FlatFileItemReaderBuilder<DailyTransaction>()
                .name("dailyTransactionReader")
                .resource(new FileSystemResource(path))
                .encoding("ISO-8859-1")
                .comments()
                .lineMapper((line, lineNumber) ->
                        TransactionLayout.parseDaily(Fields.text(line, TransactionLayout.LENGTH)))
                .build();
    }

    @Bean
    @StepScope
    public TransactionPostingProcessor transactionPostingProcessor(TransactionPostingService postingService) {
        return new TransactionPostingProcessor(postingService);
    }

    @Bean
    @StepScope
    public FlatFileItemWriter<RejectRecord> rejectWriter(@Value("#{jobParameters['rejectFile']}") String path) {
        return new FlatFileItemWriterBuilder<RejectRecord>()
                .name("rejectWriter")
                .resource(new FileSystemResource(path))
                .encoding("ISO-8859-1")
                .lineSeparator("\n")
                .lineAggregator(RejectRecord::toRecord)
                .build();
    }
}
