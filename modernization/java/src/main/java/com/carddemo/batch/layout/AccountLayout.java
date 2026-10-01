package com.carddemo.batch.layout;

import com.carddemo.batch.domain.Account;

import static com.carddemo.batch.layout.Fields.stored;
import static com.carddemo.batch.layout.Fields.text;

/** ACCOUNT-RECORD, copybook CVACT01Y (300 bytes). */
public final class AccountLayout {

    public static final int LENGTH = 300;

    private AccountLayout() {
    }

    public static Account parse(String r) {
        return new Account(
                Zoned.parseUnsigned(r.substring(0, 11)),
                stored(r, 11, 12),
                Zoned.parseSigned(r.substring(12, 24), 2),
                Zoned.parseSigned(r.substring(24, 36), 2),
                Zoned.parseSigned(r.substring(36, 48), 2),
                stored(r, 48, 58),
                stored(r, 58, 68),
                stored(r, 68, 78),
                Zoned.parseSigned(r.substring(78, 90), 2),
                Zoned.parseSigned(r.substring(90, 102), 2),
                stored(r, 102, 112),
                stored(r, 112, 122),
                stored(r, 122, 300));
    }

    public static String format(Account a) {
        return Zoned.formatUnsigned(a.getId(), 11)
                + text(a.getActiveStatus(), 1)
                + Zoned.formatSigned(a.getCurrentBalance(), 10, 2)
                + Zoned.formatSigned(a.getCreditLimit(), 10, 2)
                + Zoned.formatSigned(a.getCashCreditLimit(), 10, 2)
                + text(a.getOpenDate(), 10)
                + text(a.getExpirationDate(), 10)
                + text(a.getReissueDate(), 10)
                + Zoned.formatSigned(a.getCurrentCycleCredit(), 10, 2)
                + Zoned.formatSigned(a.getCurrentCycleDebit(), 10, 2)
                + text(a.getAddressZip(), 10)
                + text(a.getGroupId(), 10)
                + text(a.getFiller(), 178);
    }
}
