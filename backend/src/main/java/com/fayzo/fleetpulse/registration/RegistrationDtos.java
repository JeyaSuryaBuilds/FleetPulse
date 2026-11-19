package com.fayzo.fleetpulse.registration;

import jakarta.validation.constraints.*;

public final class RegistrationDtos {

    private RegistrationDtos() {}

    public record StartRequest(
            @NotBlank @Size(min = 2, max = 160) String companyName,
            @NotBlank @Email @Size(max = 190) String companyEmail,
            @NotBlank @Pattern(
                    regexp = "^\\+[1-9][0-9]{7,14}$",
                    message = "Mobile must be in international format"
            ) String mobile,
            @NotBlank @Size(min = 2, max = 160) String adminFullName,
            @NotBlank @Size(min = 10, max = 128) String password,
            @NotBlank String confirmPassword,
            @Size(max = 40) String planCode
    ) {}

    public record StartResponse(
            String registrationId,
            boolean emailSent,
            boolean mobileSent,
            int otpTtlMinutes,
            boolean complete,
            com.fayzo.fleetpulse.auth.AuthDtos.AuthResponse session
    ) {}

    public record VerifyRequest(
            @NotBlank String registrationId,
            @NotBlank @Pattern(regexp = "^[0-9]{6}$") String code
    ) {}

    public record ResendRequest(
            @NotBlank String registrationId,
            @NotBlank @Pattern(regexp = "EMAIL|MOBILE") String channel
    ) {}

    public record VerifyResponse(
            boolean emailVerified,
            boolean mobileVerified,
            boolean complete,
            com.fayzo.fleetpulse.auth.AuthDtos.AuthResponse session
    ) {}
}