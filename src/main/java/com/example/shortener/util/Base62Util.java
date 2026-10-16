package com.example.shortener.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for Base62 encoding and decoding of numeric IDs.
 * Used to generate short codes for URLs.
 */
public final class Base62Util {
    private static final char[] DIGITS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    private static final int BASE = DIGITS.length;

    private Base62Util() {}

    /**
     * Encode a positive long value to a Base62 string.
     * @param value the numeric value (must be >= 0)
     * @return Base62 representation (no leading zeros)
     */
    public static String encode(long value) {
        if (value < 0) {
            throw new IllegalArgumentException("Negative value not allowed");
        }
        if (value == 0) {
            return String.valueOf(DIGITS[0]);
        }
        StringBuilder sb = new StringBuilder();
        while (value > 0) {
            int idx = (int) (value % BASE);
            sb.append(DIGITS[idx]);
            value /= BASE;
        }
        return sb.reverse().toString();
    }

    /**
     * Decode a Base62 string back to a long value.
     * @param text Base62 encoded string
     * @return numeric value
     */
    public static long decode(String text) {
        if (text == null || text.isEmpty()) {
            throw new IllegalArgumentException("Text cannot be null or empty");
        }
        long result = 0L;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            int val = indexOf(c);
            result = result * BASE + val;
        }
        return result;
    }

    private static int indexOf(char c) {
        for (int i = 0; i < DIGITS.length; i++) {
            if (DIGITS[i] == c) {
                return i;
            }
        }
        throw new IllegalArgumentException("Invalid Base62 character: " + c);
    }
}
