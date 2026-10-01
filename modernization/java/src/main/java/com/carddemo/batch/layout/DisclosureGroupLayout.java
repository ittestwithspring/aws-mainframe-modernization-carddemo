package com.carddemo.batch.layout;

import com.carddemo.batch.domain.DisclosureGroup;
import com.carddemo.batch.domain.DisclosureGroupId;

import static com.carddemo.batch.layout.Fields.stored;

/** DIS-GROUP-RECORD, copybook CVTRA02Y (50 bytes). */
public final class DisclosureGroupLayout {

    public static final int LENGTH = 50;

    private DisclosureGroupLayout() {
    }

    public static DisclosureGroup parse(String r) {
        return new DisclosureGroup(
                new DisclosureGroupId(
                        stored(r, 0, 10),
                        stored(r, 10, 12),
                        (int) Zoned.parseUnsigned(r.substring(12, 16))),
                Zoned.parseSigned(r.substring(16, 22), 2));
    }
}
