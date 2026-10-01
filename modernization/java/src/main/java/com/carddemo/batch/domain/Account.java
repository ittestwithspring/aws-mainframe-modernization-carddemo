package com.carddemo.batch.domain;

import com.carddemo.batch.layout.Fields;
import com.carddemo.batch.layout.Zoned;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/** Account master (VSAM ACCTDATA, copybook CVACT01Y). */
@Entity
@Table(name = "account")
public class Account {

    @Id
    private Long id;

    @Column(length = 1)
    private String activeStatus;

    @Column(precision = 12, scale = 2)
    private BigDecimal currentBalance;

    @Column(precision = 12, scale = 2)
    private BigDecimal creditLimit;

    @Column(precision = 12, scale = 2)
    private BigDecimal cashCreditLimit;

    @Column(length = 10)
    private String openDate;

    @Column(length = 10)
    private String expirationDate;

    @Column(length = 10)
    private String reissueDate;

    @Column(precision = 12, scale = 2)
    private BigDecimal currentCycleCredit;

    @Column(precision = 12, scale = 2)
    private BigDecimal currentCycleDebit;

    @Column(length = 10)
    private String addressZip;

    @Column(length = 10)
    private String groupId;

    /** Unused copybook FILLER, kept so rewritten records stay byte-identical. */
    @Column(length = 178)
    private String filler;

    protected Account() {
    }

    public Account(Long id, String activeStatus, BigDecimal currentBalance, BigDecimal creditLimit,
                   BigDecimal cashCreditLimit, String openDate, String expirationDate, String reissueDate,
                   BigDecimal currentCycleCredit, BigDecimal currentCycleDebit, String addressZip,
                   String groupId, String filler) {
        this.id = id;
        this.activeStatus = activeStatus;
        this.currentBalance = currentBalance;
        this.creditLimit = creditLimit;
        this.cashCreditLimit = cashCreditLimit;
        this.openDate = openDate;
        this.expirationDate = expirationDate;
        this.reissueDate = reissueDate;
        this.currentCycleCredit = currentCycleCredit;
        this.currentCycleDebit = currentCycleDebit;
        this.addressZip = addressZip;
        this.groupId = groupId;
        this.filler = filler;
    }

    /** CBTRN02C over-limit check: cycle credit - cycle debit + amount, held in S9(09)V99. */
    public boolean wouldExceedCreditLimit(BigDecimal amount) {
        BigDecimal projected = Zoned.fit(currentCycleCredit.subtract(currentCycleDebit).add(amount), 9, 2);
        return creditLimit.compareTo(projected) < 0;
    }

    /** CBTRN02C expiration check: compares the expiration date with the first 10 bytes of the origin timestamp. */
    public boolean isExpiredAt(String originTimestamp) {
        return Fields.text(expirationDate, 10).compareTo(Fields.text(originTimestamp, 10)) < 0;
    }

    /** CBTRN02C 2800-UPDATE-ACCOUNT-REC. */
    public void post(BigDecimal amount) {
        currentBalance = Zoned.fit(currentBalance.add(amount), 10, 2);
        if (amount.signum() >= 0) {
            currentCycleCredit = Zoned.fit(currentCycleCredit.add(amount), 10, 2);
        } else {
            currentCycleDebit = Zoned.fit(currentCycleDebit.add(amount), 10, 2);
        }
    }

    /** CBACT04C 1050-UPDATE-ACCOUNT: adds the month's interest and starts a new cycle. */
    public void closeCycle(BigDecimal interest) {
        currentBalance = Zoned.fit(currentBalance.add(interest), 10, 2);
        currentCycleCredit = BigDecimal.ZERO.setScale(2);
        currentCycleDebit = BigDecimal.ZERO.setScale(2);
    }

    public Long getId() {
        return id;
    }

    public String getActiveStatus() {
        return activeStatus;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public BigDecimal getCreditLimit() {
        return creditLimit;
    }

    public BigDecimal getCashCreditLimit() {
        return cashCreditLimit;
    }

    public String getOpenDate() {
        return openDate;
    }

    public String getExpirationDate() {
        return expirationDate;
    }

    public String getReissueDate() {
        return reissueDate;
    }

    public BigDecimal getCurrentCycleCredit() {
        return currentCycleCredit;
    }

    public BigDecimal getCurrentCycleDebit() {
        return currentCycleDebit;
    }

    public String getAddressZip() {
        return addressZip;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getFiller() {
        return filler;
    }
}
