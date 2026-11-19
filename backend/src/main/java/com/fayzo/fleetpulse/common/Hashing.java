package com.fayzo.fleetpulse.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;

public final class Hashing {
    private static final SecureRandom RNG = new SecureRandom();
    private Hashing() {}
    public static String sha256(String s) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
    public static String otp6() { return String.format("%06d", RNG.nextInt(1_000_000)); }
    public static String randomToken() {
        byte[] b = new byte[48];
        RNG.nextBytes(b);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }
}
