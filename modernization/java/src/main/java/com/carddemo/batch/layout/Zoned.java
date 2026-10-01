package com.carddemo.batch.layout;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

/**
 * COBOL zoned decimal (USAGE DISPLAY) with EBCDIC-style sign overpunch in the last byte:
 * {@code {,A-I} = +0..9} and {@code },J-R = -0..9}.
 */
public final class Zoned {

    private static final String POSITIVE = "{ABCDEFGHI";
    private static final String NEGATIVE = "}JKLMNOPQR";

    private Zoned() {
    }

    /** Decodes a signed field such as {@code PIC S9(09)V99}. */
    public static BigDecimal parseSigned(String field, int scale) {
        int last = field.length() - 1;
        char signByte = field.charAt(last);
        int digit;
        boolean negative = false;
        if (signByte >= '0' && signByte <= '9') {
            digit = signByte - '0';
        } else if ((digit = POSITIVE.indexOf(signByte)) < 0) {
            digit = NEGATIVE.indexOf(signByte);
            negative = true;
        }
        if (digit < 0) {
            throw new IllegalArgumentException("Invalid zoned decimal: '" + field + "'");
        }
        String digits = field.substring(0, last) + digit;
        requireDigits(digits, field);
        BigDecimal value = new BigDecimal(new BigInteger(digits), scale);
        return negative ? value.negate() : value;
    }

    /** Decodes an unsigned field such as {@code PIC 9(11)}. */
    public static long parseUnsigned(String field) {
        requireDigits(field, field);
        return Long.parseLong(field);
    }

    public static String formatSigned(BigDecimal value, int integerDigits, int scale) {
        BigDecimal fitted = fit(value, integerDigits, scale);
        String digits = pad(fitted.unscaledValue().abs().toString(), integerDigits + scale);
        int lastDigit = digits.charAt(digits.length() - 1) - '0';
        char signByte = (fitted.signum() < 0 ? NEGATIVE : POSITIVE).charAt(lastDigit);
        return digits.substring(0, digits.length() - 1) + signByte;
    }

    public static String formatUnsigned(long value, int digits) {
        if (value < 0) {
            throw new IllegalArgumentException("Unsigned field cannot hold " + value);
        }
        String text = Long.toString(value);
        return text.length() > digits ? text.substring(text.length() - digits) : pad(text, digits);
    }

    /**
     * Stores a value into {@code PIC S9(integerDigits)V9(scale)} the way COBOL arithmetic without
     * ROUNDED / ON SIZE ERROR does: extra decimals and high-order integer digits are truncated.
     */
    public static BigDecimal fit(BigDecimal value, int integerDigits, int scale) {
        return value.setScale(scale, RoundingMode.DOWN)
                .remainder(BigDecimal.TEN.pow(integerDigits))
                .setScale(scale, RoundingMode.UNNECESSARY);
    }

    private static String pad(String digits, int length) {
        return "0".repeat(length - digits.length()) + digits;
    }

    private static void requireDigits(String digits, String field) {
        if (digits.isEmpty() || !digits.chars().allMatch(c -> c >= '0' && c <= '9')) {
            throw new IllegalArgumentException("Invalid numeric field: '" + field + "'");
        }
    }
}
