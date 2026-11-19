package com.fayzo.fleetpulse.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() {}
    public record LoginRequest(@NotBlank @Email String email, @NotBlank @Size(max = 128) String password) {}
    public record RefreshRequest(@NotBlank String refreshToken) {}
    public record UserInfo(long id, long companyId, String companyName, String fullName, String email, String role,
                           java.util.List<String> permissions) {}
    public record AuthResponse(String accessToken, String refreshToken, long expiresInSeconds, UserInfo user) {}
}
