package com.fayzo.fleetpulse.auth;

import com.fayzo.fleetpulse.audit.AuditService;
import com.fayzo.fleetpulse.auth.AuthDtos.*;
import com.fayzo.fleetpulse.common.ApiException;
import com.fayzo.fleetpulse.config.AppProperties;
import com.fayzo.fleetpulse.security.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class AuthService {
    private final String dummyHash;
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final TokenService tokens;
    private final AuditService audit;
    private final AppProperties props;

    public AuthService(JdbcTemplate jdbc, PasswordEncoder encoder, JwtService jwt, TokenService tokens, AuditService audit, AppProperties props) {
        this.jdbc = jdbc; this.encoder = encoder; this.jwt = jwt; this.tokens = tokens; this.audit = audit; this.props = props;
        this.dummyHash = encoder.encode("fleetpulse-timing-equaliser");
    }

    @Transactional
    public AuthResponse login(LoginRequest req, String ip, String ua) {
        String email = req.email().trim().toLowerCase();
        List<Map<String, Object>> rows = jdbc.queryForList("""
            SELECT u.id, u.company_id, u.password_hash, u.status, u.failed_attempts, u.locked_until, c.status cstatus
            FROM users u JOIN companies c ON c.id=u.company_id WHERE u.email=?""", email);
        if (rows.isEmpty()) {
            encoder.matches(req.password(), dummyHash); // equalise timing
            audit.loginActivity(AuthPrincipal.USER, null, email, false, "UNKNOWN_USER", ip, ua);
            throw ApiException.unauthorized("Invalid email or password");
        }
        Map<String, Object> u = rows.get(0);
        long id = ((Number) u.get("id")).longValue();
        Timestamp lockedUntil = (Timestamp) u.get("locked_until");
        if (lockedUntil != null && lockedUntil.toInstant().isAfter(Instant.now())) {
            audit.loginActivity(AuthPrincipal.USER, id, email, false, "LOCKED", ip, ua);
            throw ApiException.tooMany("Account temporarily locked. Try again later.");
        }
        if (!encoder.matches(req.password(), (String) u.get("password_hash"))) {
            int fails = ((Number) u.get("failed_attempts")).intValue() + 1;
            if (fails >= 5) jdbc.update("UPDATE users SET failed_attempts=0, locked_until=DATE_ADD(UTC_TIMESTAMP(), INTERVAL 15 MINUTE) WHERE id=?", id);
            else jdbc.update("UPDATE users SET failed_attempts=? WHERE id=?", fails, id);
            audit.loginActivity(AuthPrincipal.USER, id, email, false, "BAD_PASSWORD", ip, ua);
            throw ApiException.unauthorized("Invalid email or password");
        }
        if (!"ACTIVE".equals(u.get("status"))) throw ApiException.forbidden("This account is disabled");
        String cs = (String) u.get("cstatus");
        if ("SUSPENDED".equals(cs) || "CANCELLED".equals(cs)) throw ApiException.forbidden("This company workspace is " + cs.toLowerCase());
        jdbc.update("UPDATE users SET failed_attempts=0, locked_until=NULL, last_login_at=UTC_TIMESTAMP() WHERE id=?", id);
        audit.loginActivity(AuthPrincipal.USER, id, email, true, null, ip, ua);
        return buildResponse(id, ip, ua);
    }

    @Transactional
    public AuthResponse refresh(String refreshToken, String ip, String ua) {
        Map<String, Object> t = tokens.consume(refreshToken, AuthPrincipal.USER);
        if (t == null) throw ApiException.unauthorized("Invalid or expired session");
        return buildResponse(((Number) t.get("subject_id")).longValue(), ip, ua);
    }

    public void logout(String refreshToken) { if (refreshToken != null) tokens.revoke(refreshToken); }

    public UserInfo me(long userId) { return userInfo(userId); }

    /** Issues tokens for an already-verified user (also used right after registration completes). */
    public AuthResponse buildResponse(long userId, String ip, String ua) {
        UserInfo info = userInfo(userId);
        Integer tv = jdbc.queryForObject("SELECT token_version FROM users WHERE id=?", Integer.class, userId);
        AuthPrincipal p = new AuthPrincipal(AuthPrincipal.USER, userId, info.companyId(), info.role(), tv == null ? 0 : tv);
        return new AuthResponse(jwt.issueUserAccess(p), tokens.issueRefresh(AuthPrincipal.USER, userId, ip, ua),
                props.jwt().accessTtlMinutes() * 60L, info);
    }

    private UserInfo userInfo(long userId) {
        Map<String, Object> r = jdbc.queryForMap("""
            SELECT u.id, u.company_id, c.name cname, u.full_name, u.email, u.role_code
            FROM users u JOIN companies c ON c.id=u.company_id WHERE u.id=?""", userId);
        Role role = Role.valueOf((String) r.get("role_code"));
        List<String> perms = role.permissions().stream().map(Enum::name).sorted(Comparator.naturalOrder()).toList();
        return new UserInfo(userId, ((Number) r.get("company_id")).longValue(), (String) r.get("cname"),
                (String) r.get("full_name"), (String) r.get("email"), role.name(), perms);
    }
}
