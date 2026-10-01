package com.carddemo.batch.layout;

import com.carddemo.batch.domain.Transaction;
import com.carddemo.batch.posting.DailyTransaction;

import static com.carddemo.batch.layout.Fields.stored;
import static com.carddemo.batch.layout.Fields.text;

/** TRAN-RECORD (CVTRA05Y) and DALYTRAN-RECORD (CVTRA06Y) share this 350-byte layout. */
public final class TransactionLayout {

    public static final int LENGTH = 350;

    private TransactionLayout() {
    }

    public static DailyTransaction parseDaily(String r) {
        return new DailyTransaction(
                stored(r, 0, 16),
                stored(r, 16, 18),
                (int) Zoned.parseUnsigned(r.substring(18, 22)),
                stored(r, 22, 32),
                stored(r, 32, 132),
                Zoned.parseSigned(r.substring(132, 143), 2),
                Zoned.parseUnsigned(r.substring(143, 152)),
                stored(r, 152, 202),
                stored(r, 202, 252),
                stored(r, 252, 262),
                stored(r, 262, 278),
                r.substring(278, 304),
                r);
    }

    public static Transaction parse(String r) {
        DailyTransaction d = parseDaily(r);
        return new Transaction(d.id(), d.typeCode(), d.categoryCode(), d.source(), d.description(),
                d.amount(), d.merchantId(), d.merchantName(), d.merchantCity(), d.merchantZip(),
                d.cardNumber(), d.originTimestamp(), r.substring(304, 330));
    }

    public static String format(Transaction t) {
        return text(t.getId(), 16)
                + text(t.getTypeCode(), 2)
                + Zoned.formatUnsigned(t.getCategoryCode(), 4)
                + text(t.getSource(), 10)
                + text(t.getDescription(), 100)
                + Zoned.formatSigned(t.getAmount(), 9, 2)
                + Zoned.formatUnsigned(t.getMerchantId(), 9)
                + text(t.getMerchantName(), 50)
                + text(t.getMerchantCity(), 50)
                + text(t.getMerchantZip(), 10)
                + text(t.getCardNumber(), 16)
                + text(t.getOriginTimestamp(), 26)
                + text(t.getProcessedTimestamp(), 26)
                + text("", 20);
    }
}
