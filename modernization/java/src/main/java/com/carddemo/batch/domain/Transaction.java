package com.carddemo.batch.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/** Posted transaction (VSAM TRANSACT, copybook CVTRA05Y). */
@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @Column(length = 16)
    private String id;

    @Column(length = 2)
    private String typeCode;

    private Integer categoryCode;

    @Column(length = 10)
    private String source;

    @Column(length = 100)
    private String description;

    @Column(precision = 11, scale = 2)
    private BigDecimal amount;

    private Long merchantId;

    @Column(length = 50)
    private String merchantName;

    @Column(length = 50)
    private String merchantCity;

    @Column(length = 10)
    private String merchantZip;

    @Column(length = 16)
    private String cardNumber;

    @Column(length = 26)
    private String originTimestamp;

    @Column(length = 26)
    private String processedTimestamp;

    protected Transaction() {
    }

    public Transaction(String id, String typeCode, Integer categoryCode, String source, String description,
                       BigDecimal amount, Long merchantId, String merchantName, String merchantCity,
                       String merchantZip, String cardNumber, String originTimestamp,
                       String processedTimestamp) {
        this.id = id;
        this.typeCode = typeCode;
        this.categoryCode = categoryCode;
        this.source = source;
        this.description = description;
        this.amount = amount;
        this.merchantId = merchantId;
        this.merchantName = merchantName;
        this.merchantCity = merchantCity;
        this.merchantZip = merchantZip;
        this.cardNumber = cardNumber;
        this.originTimestamp = originTimestamp;
        this.processedTimestamp = processedTimestamp;
    }

    public String getId() {
        return id;
    }

    public String getTypeCode() {
        return typeCode;
    }

    public Integer getCategoryCode() {
        return categoryCode;
    }

    public String getSource() {
        return source;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Long getMerchantId() {
        return merchantId;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public String getMerchantCity() {
        return merchantCity;
    }

    public String getMerchantZip() {
        return merchantZip;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public String getOriginTimestamp() {
        return originTimestamp;
    }

    public String getProcessedTimestamp() {
        return processedTimestamp;
    }
}
