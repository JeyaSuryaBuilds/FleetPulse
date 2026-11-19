package com.fayzo.fleetpulse.security;

public record AuthPrincipal(String type, long subjectId, Long companyId, String role, int tokenVersion) {
    public static final String USER = "USER";
    public static final String ADMIN = "ADMIN";
    public boolean isAdmin() { return ADMIN.equals(type); }
}
