package com.carddemo.batch.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/** Card to customer/account cross reference (VSAM CARDXREF + AIX on account id, copybook CVACT03Y). */
@Entity
@Table(name = "card_xref", indexes = @Index(name = "card_xref_account_idx", columnList = "accountId"))
public class CardXref {

    @Id
    @Column(length = 16)
    private String cardNumber;

    private Long customerId;

    private Long accountId;

    protected CardXref() {
    }

    public CardXref(String cardNumber, Long customerId, Long accountId) {
        this.cardNumber = cardNumber;
        this.customerId = customerId;
        this.accountId = accountId;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public Long getAccountId() {
        return accountId;
    }
}
