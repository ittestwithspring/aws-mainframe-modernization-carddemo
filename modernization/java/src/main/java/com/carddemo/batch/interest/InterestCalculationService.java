package com.carddemo.batch.interest;

import com.carddemo.batch.domain.Account;
import com.carddemo.batch.domain.AccountRepository;
import com.carddemo.batch.domain.CardXref;
import com.carddemo.batch.domain.CardXrefRepository;
import com.carddemo.batch.domain.DisclosureGroup;
import com.carddemo.batch.domain.DisclosureGroupId;
import com.carddemo.batch.domain.DisclosureGroupRepository;
import com.carddemo.batch.domain.Transaction;
import com.carddemo.batch.domain.TransactionCategoryBalance;
import com.carddemo.batch.domain.TransactionCategoryBalanceId;
import com.carddemo.batch.domain.TransactionCategoryBalanceRepository;
import com.carddemo.batch.layout.Fields;
import com.carddemo.batch.layout.TransactionCategoryBalanceLayout;
import com.carddemo.batch.layout.Zoned;
import com.carddemo.batch.support.AbendException;
import com.carddemo.batch.support.BatchClock;
import com.carddemo.batch.support.Sysout;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.function.Consumer;

/** Monthly interest calculation (COBOL CBACT04C). */
@Service
public class InterestCalculationService {

    private static final BigDecimal MONTHS_TIMES_PERCENT = BigDecimal.valueOf(1200);
    private static final Sort BALANCE_KEY_ORDER =
            Sort.by("id.accountId", "id.typeCode", "id.categoryCode");

    private final TransactionCategoryBalanceRepository categoryBalances;
    private final AccountRepository accounts;
    private final CardXrefRepository cardXrefs;
    private final DisclosureGroupRepository disclosureGroups;
    private final BatchClock clock;
    private final Sysout sysout;

    public InterestCalculationService(TransactionCategoryBalanceRepository categoryBalances,
                                      AccountRepository accounts, CardXrefRepository cardXrefs,
                                      DisclosureGroupRepository disclosureGroups, BatchClock clock,
                                      Sysout sysout) {
        this.categoryBalances = categoryBalances;
        this.accounts = accounts;
        this.cardXrefs = cardXrefs;
        this.disclosureGroups = disclosureGroups;
        this.clock = clock;
        this.sysout = sysout;
    }

    /**
     * Walks the category balances in key order and, on each change of account, adds the previous
     * account's interest to its balance and resets its cycle totals. Each non-zero interest amount
     * becomes a system transaction.
     *
     * @param runDate the JCL PARM; its first 10 bytes prefix the generated transaction ids
     */
    public void calculate(String runDate, Consumer<Transaction> systemTransactions) {
        String tranIdPrefix = Fields.text(runDate, 10);
        int tranIdSuffix = 0;
        Account account = null;
        CardXref xref = null;
        BigDecimal totalInterest = BigDecimal.ZERO;

        for (TransactionCategoryBalance balance : categoryBalances.findAll(BALANCE_KEY_ORDER)) {
            sysout.display(TransactionCategoryBalanceLayout.format(balance));
            TransactionCategoryBalanceId key = balance.getId();
            if (account == null || !account.getId().equals(key.accountId())) {
                if (account != null) {
                    account.closeCycle(totalInterest);
                }
                totalInterest = BigDecimal.ZERO;
                account = findAccount(key.accountId());
                xref = findXref(key.accountId());
            }
            BigDecimal rate = interestRate(account.getGroupId(), key);
            if (rate.signum() != 0) {
                BigDecimal monthlyInterest = Zoned.fit(
                        balance.getBalance().multiply(rate).divide(MONTHS_TIMES_PERCENT, 10, RoundingMode.DOWN),
                        9, 2);
                totalInterest = Zoned.fit(totalInterest.add(monthlyInterest), 9, 2);
                tranIdSuffix++;
                systemTransactions.accept(interestTransaction(
                        tranIdPrefix + String.format("%06d", tranIdSuffix), account, xref, monthlyInterest));
            }
        }
        // CBACT04C never updates the last account: its end-of-file 1050-UPDATE-ACCOUNT branch is
        // unreachable because the PERFORM UNTIL loop exits first. Kept for parity; see docs/inventory.md.
    }

    private Account findAccount(Long accountId) {
        return accounts.findById(accountId).orElseThrow(() -> {
            sysout.display("ACCOUNT NOT FOUND: " + Zoned.formatUnsigned(accountId, 11));
            return new AbendException("Account " + accountId + " not found");
        });
    }

    private CardXref findXref(Long accountId) {
        return cardXrefs.findFirstByAccountIdOrderByCardNumberAsc(accountId).orElseThrow(() -> {
            sysout.display("ACCOUNT NOT FOUND: " + Zoned.formatUnsigned(accountId, 11));
            return new AbendException("No card cross reference for account " + accountId);
        });
    }

    /** 1200-GET-INTEREST-RATE: the account's group, falling back to the DEFAULT group. */
    private BigDecimal interestRate(String groupId, TransactionCategoryBalanceId key) {
        Optional<DisclosureGroup> group = disclosureGroups.findById(
                new DisclosureGroupId(groupId, key.typeCode(), key.categoryCode()));
        if (group.isEmpty()) {
            sysout.display("DISCLOSURE GROUP RECORD MISSING");
            sysout.display("TRY WITH DEFAULT GROUP CODE");
            group = disclosureGroups.findById(
                    new DisclosureGroupId(DisclosureGroup.DEFAULT_GROUP, key.typeCode(), key.categoryCode()));
        }
        return group.orElseThrow(() -> {
            sysout.display("ERROR READING DEFAULT DISCLOSURE GROUP");
            return new AbendException("No DEFAULT disclosure group for " + key);
        }).getInterestRate();
    }

    private Transaction interestTransaction(String id, Account account, CardXref xref, BigDecimal amount) {
        String now = clock.db2Timestamp();
        return new Transaction(id, "01", 5, "System",
                "Int. for a/c " + Zoned.formatUnsigned(account.getId(), 11),
                amount, 0L, "", "", "", xref.getCardNumber(), now, now);
    }
}
