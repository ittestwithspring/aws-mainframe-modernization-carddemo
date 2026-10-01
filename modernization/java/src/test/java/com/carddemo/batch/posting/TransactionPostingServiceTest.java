package com.carddemo.batch.posting;

import com.carddemo.batch.domain.Account;
import com.carddemo.batch.domain.AccountRepository;
import com.carddemo.batch.domain.CardXref;
import com.carddemo.batch.domain.CardXrefRepository;
import com.carddemo.batch.domain.TransactionCategoryBalance;
import com.carddemo.batch.domain.TransactionCategoryBalanceId;
import com.carddemo.batch.domain.TransactionCategoryBalanceRepository;
import com.carddemo.batch.domain.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "carddemo.clock.fixed=2022-07-18T10:11:12Z")
@Transactional
class TransactionPostingServiceTest {

    private static final String CARD = "4111111111111111";
    private static final long ACCOUNT_ID = 7L;

    @Autowired
    private TransactionPostingService service;
    @Autowired
    private AccountRepository accounts;
    @Autowired
    private CardXrefRepository cardXrefs;
    @Autowired
    private TransactionCategoryBalanceRepository categoryBalances;
    @Autowired
    private TransactionRepository transactions;

    @BeforeEach
    void seed() {
        cardXrefs.save(new CardXref(CARD, 1L, ACCOUNT_ID));
        accounts.save(new Account(ACCOUNT_ID, "Y", money("100.00"), money("1000.00"), money("500.00"),
                "2020-01-01", "2025-12-31", "2025-12-31", money("200.00"), money("50.00"), "12345",
                "A000000000", ""));
    }

    @Test
    void unknownCardIsRejectedWith100() {
        Optional<RejectRecord> reject = service.process(daily("T1", "9999999999999999", "10.00", "2022-07-18"),
                new PostingRun());
        assertThat(reject).map(RejectRecord::reasonCode).contains(TransactionPostingService.INVALID_CARD);
    }

    @Test
    void accountMissingIsRejectedWith101() {
        cardXrefs.save(new CardXref("4000000000000002", 1L, 99L));
        Optional<RejectRecord> reject = service.process(daily("T1", "4000000000000002", "10.00", "2022-07-18"),
                new PostingRun());
        assertThat(reject).map(RejectRecord::reasonCode).contains(TransactionPostingService.ACCOUNT_NOT_FOUND);
    }

    @Test
    void expirationReasonReplacesOverLimitReason() {
        Optional<RejectRecord> reject = service.process(daily("T1", CARD, "900.00", "2026-01-01"),
                new PostingRun());
        assertThat(reject).map(RejectRecord::reasonCode).contains(TransactionPostingService.ACCOUNT_EXPIRED);
        assertThat(reject.orElseThrow().toRecord()).hasSize(RejectRecord.LENGTH)
                .contains("0103TRANSACTION RECEIVED AFTER ACCT EXPIRATION");
    }

    @Test
    void overLimitIsRejectedWith102() {
        Optional<RejectRecord> reject = service.process(daily("T1", CARD, "850.01", "2022-07-18"),
                new PostingRun());
        assertThat(reject).map(RejectRecord::reasonCode).contains(TransactionPostingService.OVER_LIMIT);
    }

    @Test
    void postingUpdatesAccountAndCreatesCategoryBalanceWithInheritedFiller() {
        PostingRun run = new PostingRun();
        run.categoryBalanceRead("0".repeat(22));

        assertThat(service.process(daily("T1", CARD, "-25.50", "2022-07-18"), run)).isEmpty();

        Account account = accounts.findById(ACCOUNT_ID).orElseThrow();
        assertThat(account.getCurrentBalance()).isEqualByComparingTo("74.50");
        assertThat(account.getCurrentCycleCredit()).isEqualByComparingTo("200.00");
        assertThat(account.getCurrentCycleDebit()).isEqualByComparingTo("24.50");
        TransactionCategoryBalance balance = categoryBalances
                .findById(new TransactionCategoryBalanceId(ACCOUNT_ID, "01", 1)).orElseThrow();
        assertThat(balance.getBalance()).isEqualByComparingTo("-25.50");
        assertThat(balance.getFiller()).isEqualTo("0".repeat(22));
        assertThat(transactions.findById("T1").orElseThrow().getProcessedTimestamp())
                .isEqualTo("2022-07-18-10.11.12.000000");
    }

    private static DailyTransaction daily(String id, String card, String amount, String originDate) {
        return new DailyTransaction(id, "01", 1, "POS TERM", "test", money(amount), 1L, "m", "c", "z", card,
                originDate + "-00.00.00.000000", "R".repeat(350));
    }

    private static BigDecimal money(String value) {
        return new BigDecimal(value);
    }
}
