package com.carddemo.batch.posting;

import java.math.BigDecimal;

/** One DALYTRAN input record (copybook CVTRA06Y); {@code record} is the raw 350-byte line. */
public record DailyTransaction(
        String id,
        String typeCode,
        int categoryCode,
        String source,
        String description,
        BigDecimal amount,
        long merchantId,
        String merchantName,
        String merchantCity,
        String merchantZip,
        String cardNumber,
        String originTimestamp,
        String record) {
}
