package com.carddemo.batch.layout;

/** Helpers for fixed-width alphanumeric (PIC X) fields. */
public final class Fields {

    private Fields() {
    }

    /** Pads with spaces or truncates to exactly {@code length} characters, like a MOVE to PIC X(n). */
    public static String text(String value, int length) {
        String v = value == null ? "" : value;
        return v.length() >= length ? v.substring(0, length) : v + " ".repeat(length - v.length());
    }

    /** Value as stored in an entity: trailing pad spaces are dropped, leading spaces are kept. */
    public static String stored(String record, int start, int end) {
        return record.substring(start, end).stripTrailing();
    }
}
