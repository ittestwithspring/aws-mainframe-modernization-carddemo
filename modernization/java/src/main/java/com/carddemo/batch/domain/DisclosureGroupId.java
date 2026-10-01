package com.carddemo.batch.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record DisclosureGroupId(
        @Column(length = 10) String groupId,
        @Column(length = 2) String typeCode,
        Integer categoryCode) {
}
