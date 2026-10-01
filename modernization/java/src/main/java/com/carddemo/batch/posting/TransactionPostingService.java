package com.carddemo.batch.posting;

import com.carddemo.batch.domain.Account;
import com.carddemo.batch.domain.AccountRepository;
import com.carddemo.batch.domain.CardXref;
import com.carddemo.batch.domain.CardXrefRepository;
import com.carddemo.batch.domain.Transaction;
import com.carddemo.batch.domain.TransactionCategoryBalance;
import com.carddemo.batch.domain.TransactionCategoryBalanceId;
import com.carddemo.batch.domain.TransactionCategoryBalanceRepository;
import com.carddemo.batch.domain.TransactionRepository;
import com.carddemo.batch.layout.TransactionCategoryBalanceLayout;
import com.carddemo.batch.layout.Zoned;
import com.carddemo.batch.support.AbendException;
import com.carddemo.batch.support.BatchClock;
import com.carddemo.batch.support.Sysout;
import org.springframework.stereotype.Service;

import java.util.Optional;

/** Validates and posts one daily transaction (CBTRN02C 1500-VALIDATE-TRAN and 2000-POST-TRANSACTION). */
@Service
public class TransactionPostingService {

    static final int INVALID_CARD = 100;
    static final int ACCOUNT_NOT_FOUND = 101;
    static final int OVER_LIMIT = 102;
    static final int ACCOUNT_EXPIRED = 103;

    private final CardXrefRepository cardXrefs;
    private final AccountRepository accounts;
    private final TransactionCategoryBalanceRepository categoryBalances;
    private final TransactionRepository transactions;
    private final BatchClock clock;
    private final Sysout sysout;

    public TransactionPostingService(CardXrefRepository cardXrefs, AccountRepository accounts,
                                     TransactionCategoryBalanceRepository categoryBalances,
                                     TransactionRepository transactions, BatchClock clock, Sysout sysout) {
        this.cardXrefs = cardXrefs;
        this.accounts = accounts;
        this.categoryBalances = categoryBalances;
        this.transactions = transactions;
        this.clock = clock;
        this.sysout = sysout;
    }

    /** Returns the reject record, or empty when the transaction was posted. */
    public Optional<RejectRecord> process(DailyTransaction tran, PostingRun run) {
        Optional<CardXref> xref = cardXrefs.findById(tran.cardNumber());
        if (xref.isEmpty()) {
            return reject(tran, INVALID_CARD, "INVALID CARD NUMBER FOUND");
        }
        Optional<Account> account = accounts.findById(xref.get().getAccountId());
        if (account.isEmpty()) {
            return reject(tran, ACCOUNT_NOT_FOUND, "ACCOUNT RECORD NOT FOUND");
        }
        // Both checks run; when both fail the expiration reason wins, as in CBTRN02C.
        Optional<RejectRecord> rejection = Optional.empty();
        if (account.get().wouldExceedCreditLimit(tran.amount())) {
            rejection = reject(tran, OVER_LIMIT, "OVERLIMIT TRANSACTION");
        }
        if (account.get().isExpiredAt(tran.originTimestamp())) {
            rejection = reject(tran, ACCOUNT_EXPIRED, "TRANSACTION RECEIVED AFTER ACCT EXPIRATION");
        }
        if (rejection.isEmpty()) {
            post(tran, xref.get(), account.get(), run);
        }
        return rejection;
    }

    private void post(DailyTransaction tran, CardXref xref, Account account, PostingRun run) {
        String processedTimestamp = clock.db2Timestamp();
        updateCategoryBalance(xref.getAccountId(), tran, run);
        account.post(tran.amount());
        if (transactions.existsById(tran.id())) {
            sysout.display("ERROR WRITING TO TRANSACTION FILE");
            throw new AbendException("Duplicate transaction id " + tran.id());
        }
        transactions.save(new Transaction(tran.id(), tran.typeCode(), tran.categoryCode(), tran.source(),
                tran.description(), tran.amount(), tran.merchantId(), tran.merchantName(), tran.merchantCity(),
                tran.merchantZip(), tran.cardNumber(), tran.originTimestamp(), processedTimestamp));
    }

    private void updateCategoryBalance(Long accountId, DailyTransaction tran, PostingRun run) {
        TransactionCategoryBalanceId id =
                new TransactionCategoryBalanceId(accountId, tran.typeCode(), tran.categoryCode());
        Optional<TransactionCategoryBalance> existing = categoryBalances.findById(id);
        if (existing.isPresent()) {
            run.categoryBalanceRead(existing.get().getFiller());
            existing.get().add(tran.amount());
        } else {
            sysout.display("TCATBAL record not found for key : "
                    + TransactionCategoryBalanceLayout.key(id) + ".. Creating.");
            categoryBalances.save(new TransactionCategoryBalance(
                    id, Zoned.fit(tran.amount(), 9, 2), run.lastCategoryBalanceFiller()));
        }
    }

    private static Optional<RejectRecord> reject(DailyTransaction tran, int code, String description) {
        return Optional.of(new RejectRecord(tran.record(), code, description));
    }
}
