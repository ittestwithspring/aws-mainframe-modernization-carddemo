package com.carddemo.batch.posting;

import org.springframework.batch.item.ItemProcessor;

/**
 * Posts each daily transaction as it is processed and passes only rejects on to the writer.
 * Posting happens here rather than in the writer so that every record sees the balance updates of
 * the records before it, matching CBTRN02C's record-at-a-time loop.
 */
public class TransactionPostingProcessor implements ItemProcessor<DailyTransaction, RejectRecord> {

    private final TransactionPostingService postingService;
    private final PostingRun run = new PostingRun();

    public TransactionPostingProcessor(TransactionPostingService postingService) {
        this.postingService = postingService;
    }

    @Override
    public RejectRecord process(DailyTransaction transaction) {
        return postingService.process(transaction, run).orElse(null);
    }
}
