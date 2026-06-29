package com.ThanhND05.url_shortener.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Hashing utilities for URLs, IPs, user-agents, and API keys.
 * Uses SHA-256 for consistent, non-reversible hashing.
 */
public final class HashUtil {

    private HashUtil() {
        // utility class
    }

    /**
     * SHA-256 hash of a string, returned as a lowercase hex string (64 chars).
     */
    public static String sha256Hex(String input) {
        if (input == null) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * SHA-256 hash of a string, returned as raw bytes (32 bytes).
     * Suitable for storing in BYTEA columns.
     */
    public static byte[] sha256Bytes(String input) {
        if (input == null) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(input.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
