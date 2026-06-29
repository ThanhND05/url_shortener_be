package com.ThanhND05.url_shortener.common.util;

/**
 * Base62 encoder/decoder for generating short, URL-safe codes from numeric IDs.
 * Alphabet: 0-9, A-Z, a-z (62 characters).
 * <p>
 * Used to convert the PostgreSQL sequence value (link.short_code_seq)
 * into a compact short code like "2bJk9".
 */
public final class Base62Encoder {

    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int BASE = ALPHABET.length(); // 62

    private Base62Encoder() {
        // utility class
    }

    /**
     * Encode a positive long value to a Base62 string.
     *
     * @param value must be >= 0
     * @return Base62-encoded string (e.g., 100000 → "q0U")
     */
    public static String encode(long value) {
        if (value < 0) {
            throw new IllegalArgumentException("Value must be non-negative: " + value);
        }
        if (value == 0) {
            return String.valueOf(ALPHABET.charAt(0));
        }

        StringBuilder sb = new StringBuilder();
        while (value > 0) {
            sb.append(ALPHABET.charAt((int) (value % BASE)));
            value /= BASE;
        }
        return sb.reverse().toString();
    }

    /**
     * Decode a Base62 string back to a long value.
     */
    public static long decode(String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            throw new IllegalArgumentException("Encoded string must not be null or empty");
        }
        long value = 0;
        for (char c : encoded.toCharArray()) {
            int idx = ALPHABET.indexOf(c);
            if (idx < 0) {
                throw new IllegalArgumentException("Invalid Base62 character: " + c);
            }
            value = value * BASE + idx;
        }
        return value;
    }
}
