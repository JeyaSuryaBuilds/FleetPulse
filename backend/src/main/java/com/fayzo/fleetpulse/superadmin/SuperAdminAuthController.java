package com.fayzo.fleetpulse.superadmin;

import com.fayzo.fleetpulse.audit.AuditService;
import com.fayzo.fleetpulse.common.ApiException;
import com.fayzo.fleetpulse.config.AppProperties;
import com.fayzo.fleetpulse.security.AuthPrincipal;
import com.fayzo.fleetpulse.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Single-password Super Admin login at POST /api/v1/admin/auth/login. Lockout + audit on every attempt. */
@RestController
@RequestMapping("/api/v1/admin/auth")
public class SuperAdminAuthController {
    public record LoginRequest(@NotBlank @Size(max = 128) String password) {}
    public record LoginResponse(String accessToken, long expiresInSeconds, String displayName) {}

    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final AuditService audit;
    private final AppProperties props;

    public SuperAdminAuthController(JdbcTemplate jdbc, PasswordEncoder encoder, JwtService jwt, AuditService audit, AppProperties props) {
        this.jdbc = jdbc; this.encoder = encoder; this.jwt = jwt; this.audit = audit; this.props = props;
    }

    @PostMapping("/login")
    @Transactional(noRollbackFor = ApiException.class)
    public LoginResponse login(@Valid @RequestBody LoginRequest r, HttpServletRequest http) {
        String ip = http.getRemoteAddr(), ua = http.getHeader("User-Agent");
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM platform_admins ORDER BY id LIMIT 1 FOR UPDATE");
        if (rows.isEmpty()) throw ApiException.unauthorized("Invalid password");
        Map<String, Object> a = rows.get(0);
        long id = ((Number) a.get("id")).longValue();
        Timestamp lock = (Timestamp) a.get("locked_until");
        if (lock != null && lock.toInstant().isAfter(Instant.now())) {
            audit.loginActivity(AuthPrincipal.ADMIN, id, "superadmin", false, "LOCKED", ip, ua);
            throw ApiException.tooMany("Too many failed attempts. Try again later.");
        }
        if (!encoder.matches(r.password(), (String) a.get("password_hash"))) {
            int fails = ((Number) a.get("failed_attempts")).intValue() + 1;
            if (fails >= props.superadmin().maxFailedAttempts())
                jdbc.update("UPDATE platform_admins SET failed_attempts=0, locked_until=DATE_ADD(UTC_TIMESTAMP(), INTERVAL ? MINUTE) WHERE id=?", props.superadmin().lockoutMinutes(), id);
            else jdbc.update("UPDATE platform_admins SET failed_attempts=? WHERE id=?", fails, id);
            audit.loginActivity(AuthPrincipal.ADMIN, id, "superadmin", false, "BAD_PASSWORD", ip, ua);
            throw ApiException.unauthorized("Invalid password");
        }
        jdbc.update("UPDATE platform_admins SET failed_attempts=0, locked_until=NULL, last_login_at=UTC_TIMESTAMP() WHERE id=?", id);
        audit.loginActivity(AuthPrincipal.ADMIN, id, "superadmin", true, null, ip, ua);
        audit.log(null, "ADMIN", id, "ADMIN_LOGIN", null, null, ip);
        var principal = new AuthPrincipal(AuthPrincipal.ADMIN, id, null, "SUPER_ADMIN", ((Number) a.get("token_version")).intValue());
        return new LoginResponse(jwt.issueAdminAccess(principal), props.superadmin().sessionTtlMinutes() * 60L, (String) a.get("display_name"));
    }

    /** Invalidates every outstanding admin token (session management). */
    @PostMapping("/logout-all")
    public Map<String, Boolean> logoutAll(@org.springframework.security.core.annotation.AuthenticationPrincipal AuthPrincipal p) {
        if (p == null || !p.isAdmin()) throw ApiException.unauthorized("Authentication required");
        jdbc.update("UPDATE platform_admins SET token_version=token_version+1 WHERE id=?", p.subjectId());
        return Map.of("ok", true);
    }
}
