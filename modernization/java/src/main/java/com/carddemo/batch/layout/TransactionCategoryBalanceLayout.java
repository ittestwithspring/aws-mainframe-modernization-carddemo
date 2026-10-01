package com.carddemo.batch.layout;

import com.carddemo.batch.domain.TransactionCategoryBalance;
import com.carddemo.batch.domain.TransactionCategoryBalanceId;

import static com.carddemo.batch.layout.Fields.stored;
import static com.carddemo.batch.layout.Fields.text;

/** TRAN-CAT-BAL-RECORD, copybook CVTRA01Y (50 bytes). */
public final class TransactionCategoryBalanceLayout {

    public static final int LENGTH = 50;
    public static final int FILLER_LENGTH = 22;

    private TransactionCategoryBalanceLayout() {
    }

    public static TransactionCategoryBalance parse(String r) {
        return new TransactionCategoryBalance(
                new TransactionCategoryBalanceId(
                        Zoned.parseUnsigned(r.substring(0, 11)),
                        stored(r, 11, 13),
                        (int) Zoned.parseUnsigned(r.substring(13, 17))),
                Zoned.parseSigned(r.substring(17, 28), 2),
                stored(r, 28, 50));
    }

    public static String format(TransactionCategoryBalance b) {
        return key(b.getId())
                + Zoned.formatSigned(b.getBalance(), 9, 2)
                + text(b.getFiller(), FILLER_LENGTH);
    }

    /** TRAN-CAT-KEY: account id, type code, category code. */
    public static String key(TransactionCategoryBalanceId id) {
        return Zoned.formatUnsigned(id.accountId(), 11)
                + text(id.typeCode(), 2)
                + Zoned.formatUnsigned(id.categoryCode(), 4);
    }
}
