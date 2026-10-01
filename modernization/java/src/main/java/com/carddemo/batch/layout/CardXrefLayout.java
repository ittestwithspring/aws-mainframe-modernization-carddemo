package com.carddemo.batch.layout;

import com.carddemo.batch.domain.CardXref;

import static com.carddemo.batch.layout.Fields.stored;

/** CARD-XREF-RECORD, copybook CVACT03Y (50 bytes). */
public final class CardXrefLayout {

    public static final int LENGTH = 50;

    private CardXrefLayout() {
    }

    public static CardXref parse(String r) {
        return new CardXref(
                stored(r, 0, 16),
                Zoned.parseUnsigned(r.substring(16, 25)),
                Zoned.parseUnsigned(r.substring(25, 36)));
    }
}
