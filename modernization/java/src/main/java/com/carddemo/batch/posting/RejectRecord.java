package com.carddemo.batch.posting;

import com.carddemo.batch.layout.Fields;

/** DALYREJS record: the original daily transaction followed by an 80-byte validation trailer. */
public record RejectRecord(String dailyRecord, int reasonCode, String reasonDescription) {

    public static final int LENGTH = 430;

    public String toRecord() {
        return dailyRecord + String.format("%04d", reasonCode) + Fields.text(reasonDescription, 76);
    }
}
