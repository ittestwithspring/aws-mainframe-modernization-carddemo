package com.carddemo.batch.posting;

import com.carddemo.batch.layout.TransactionCategoryBalanceLayout;

/**
 * State carried between records within one posting run.
 *
 * <p>CBTRN02C builds new TCATBAL records with INITIALIZE, which leaves FILLER untouched, so a new
 * record inherits the FILLER of the last balance record read in the run (spaces before any read).
 */
public class PostingRun {

    private String lastCategoryBalanceFiller = " ".repeat(TransactionCategoryBalanceLayout.FILLER_LENGTH);

    public String lastCategoryBalanceFiller() {
        return lastCategoryBalanceFiller;
    }

    public void categoryBalanceRead(String filler) {
        lastCategoryBalanceFiller = filler;
    }
}
