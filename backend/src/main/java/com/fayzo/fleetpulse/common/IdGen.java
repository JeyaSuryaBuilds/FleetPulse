package com.fayzo.fleetpulse.common;

import java.security.SecureRandom;

/** Non-guessable business identifiers (tracking IDs are public, so they must not be sequential). */
public final class IdGen {
    private static final SecureRandom RNG = new SecureRandom();
    private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private IdGen() {}
    private static String rand(int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) sb.append(ALPHABET[RNG.nextInt(ALPHABET.length)]);
        return sb.toString();
    }
    public static String trackingId() { return "FP" + rand(10); }
    public static String code(String prefix) { return prefix + "-" + rand(8); }
}
