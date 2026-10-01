package com.carddemo.batch.domain;

import com.carddemo.batch.layout.Zoned;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/** Balance per account, transaction type and category (VSAM TCATBALF, copybook CVTRA01Y). */
@Entity
@Table(name = "transaction_category_balance")
public class TransactionCategoryBalance {

    @EmbeddedId
    private TransactionCategoryBalanceId id;

    @Column(precision = 11, scale = 2)
    private BigDecimal balance;

    /** Unused copybook FILLER, kept so rewritten records stay byte-identical. */
    @Column(length = 22)
    private String filler;

    protected TransactionCategoryBalance() {
    }

    public TransactionCategoryBalance(TransactionCategoryBalanceId id, BigDecimal balance, String filler) {
        this.id = id;
        this.balance = balance;
        this.filler = filler;
    }

    public void add(BigDecimal amount) {
        balance = Zoned.fit(balance.add(amount), 9, 2);
    }

    public TransactionCategoryBalanceId getId() {
        return id;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public String getFiller() {
        return filler;
    }
}
