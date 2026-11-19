package com.fayzo.fleetpulse.auth;

import com.fayzo.fleetpulse.auth.AuthDtos.*;
import com.fayzo.fleetpulse.security.AuthPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService auth;
    public AuthController(AuthService auth) { this.auth = auth; }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest r, HttpServletRequest http) {
        return auth.login(r, http.getRemoteAddr(), http.getHeader("User-Agent"));
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest r, HttpServletRequest http) {
        return auth.refresh(r.refreshToken(), http.getRemoteAddr(), http.getHeader("User-Agent"));
    }

    @PostMapping("/logout")
    public Map<String, Boolean> logout(@RequestBody(required = false) RefreshRequest r) {
        auth.logout(r == null ? null : r.refreshToken());
        return Map.of("ok", true);
    }

    @GetMapping("/me")
    public UserInfo me(@AuthenticationPrincipal AuthPrincipal p) {
        if (p == null || p.isAdmin()) throw com.fayzo.fleetpulse.common.ApiException.unauthorized("Authentication required");
        return auth.me(p.subjectId());
    }
}
