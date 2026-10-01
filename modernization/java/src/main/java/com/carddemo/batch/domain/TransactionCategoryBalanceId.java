package com.carddemo.batch.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record TransactionCategoryBalanceId(
        Long accountId,
        @Column(length = 2) String typeCode,
        Integer categoryCode) {
}
