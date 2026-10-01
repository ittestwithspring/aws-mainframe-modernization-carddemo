package com.carddemo.batch.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/** Interest rate per account group, transaction type and category (VSAM DISCGRP, copybook CVTRA02Y). */
@Entity
@Table(name = "disclosure_group")
public class DisclosureGroup {

    public static final String DEFAULT_GROUP = "DEFAULT";

    @EmbeddedId
    private DisclosureGroupId id;

    @Column(precision = 6, scale = 2)
    private BigDecimal interestRate;

    protected DisclosureGroup() {
    }

    public DisclosureGroup(DisclosureGroupId id, BigDecimal interestRate) {
        this.id = id;
        this.interestRate = interestRate;
    }

    public DisclosureGroupId getId() {
        return id;
    }

    public BigDecimal getInterestRate() {
        return interestRate;
    }
}
