package com.carddemo.batch.support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Source of "now" for batch timestamps. {@code carddemo.clock.fixed} (ISO instant, UTC) pins it,
 * which the parity harness uses in place of GnuCOBOL's COB_CURRENT_DATE.
 */
@Component
public class BatchClock {

    private static final DateTimeFormatter DB2_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SS");

    private final Clock clock;

    public BatchClock(@Value("${carddemo.clock.fixed:}") String fixedInstant) {
        this.clock = fixedInstant.isBlank()
                ? Clock.systemDefaultZone()
                : Clock.fixed(Instant.parse(fixedInstant), ZoneOffset.UTC);
    }

    /** Z-GET-DB2-FORMAT-TIMESTAMP: {@code YYYY-MM-DD-HH.MM.SS.hh0000} (hundredths, then zeros). */
    public String db2Timestamp() {
        return DB2_TIMESTAMP.format(LocalDateTime.now(clock)) + "0000";
    }
}
