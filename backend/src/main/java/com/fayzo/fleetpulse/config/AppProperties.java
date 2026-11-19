package com.fayzo.fleetpulse.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fleetpulse")
public record AppProperties(
        Jwt jwt,
        Cors cors,
        SuperAdmin superadmin,
        Otp otp,
        Email email,
        Sms sms,
        RateLimit rateLimit,
        int trialDays
) {

    public record Jwt(
            String secret,
            int accessTtlMinutes,
            int refreshTtlDays,
            String issuer
    ) {}

    public record Cors(
            java.util.List<String> allowedOrigins
    ) {}

    public record SuperAdmin(
            String bootstrapPassword,
            int maxFailedAttempts,
            int lockoutMinutes,
            int sessionTtlMinutes
    ) {}

    public record Otp(
            int ttlMinutes,
            int maxAttempts,
            int resendCooldownSeconds,
            boolean devBypass
    ) {}

    public record Email(
            String from,
            boolean enabled
    ) {}

    public record Sms(
            boolean enabled,
            String twilioAccountSid,
            String twilioAuthToken,
            String twilioFrom
    ) {}

    public record RateLimit(
            int publicPerMinute,
            int authPerMinute
    ) {}
}