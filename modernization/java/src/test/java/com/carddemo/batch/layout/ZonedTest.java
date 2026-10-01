package com.carddemo.batch.layout;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ZonedTest {

    @Test
    void decodesOverpunchedSign() {
        assertThat(Zoned.parseSigned("0000116487G", 2)).isEqualByComparingTo("11648.77");
        assertThat(Zoned.parseSigned("0000000047{", 2)).isEqualByComparingTo("4.70");
        assertThat(Zoned.parseSigned("0000000047Q", 2)).isEqualByComparingTo("-4.78");
        assertThat(Zoned.parseSigned("0000000000}", 2)).isEqualByComparingTo("0");
        assertThat(Zoned.parseSigned("01500", 2)).isEqualByComparingTo("15.00");
    }

    @Test
    void encodesOverpunchedSign() {
        assertThat(Zoned.formatSigned(new BigDecimal("11648.77"), 9, 2)).isEqualTo("0000116487G");
        assertThat(Zoned.formatSigned(new BigDecimal("-4.78"), 9, 2)).isEqualTo("0000000047Q");
        assertThat(Zoned.formatSigned(BigDecimal.ZERO, 10, 2)).isEqualTo("00000000000{");
    }

    @Test
    void fitTruncatesLikeCobolWithoutRounded() {
        assertThat(Zoned.fit(new BigDecimal("14.560875"), 9, 2)).isEqualTo(new BigDecimal("14.56"));
        assertThat(Zoned.fit(new BigDecimal("-14.569"), 9, 2)).isEqualTo(new BigDecimal("-14.56"));
        assertThat(Zoned.fit(new BigDecimal("1234567890.12"), 9, 2)).isEqualTo(new BigDecimal("234567890.12"));
    }

    @Test
    void formatsUnsignedWithHighOrderTruncation() {
        assertThat(Zoned.formatUnsigned(5, 4)).isEqualTo("0005");
        assertThat(Zoned.formatUnsigned(123456, 4)).isEqualTo("3456");
    }

    @Test
    void rejectsNonNumericData() {
        assertThatThrownBy(() -> Zoned.parseSigned("00A0000000{", 2)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Zoned.parseUnsigned("12 4")).isInstanceOf(IllegalArgumentException.class);
    }
}
