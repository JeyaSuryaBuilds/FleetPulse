package com.fayzo.fleetpulse.auth;

import com.fayzo.fleetpulse.common.Hashing;
import com.fayzo.fleetpulse.config.AppProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/** Opaque, hashed, rotating refresh tokens. Only the SHA-256 of a token is stored. */
@Service
public class TokenService {
    private final JdbcTemplate jdbc;
    private final AppProperties props;

    public TokenService(JdbcTemplate jdbc, AppProperties props) { this.jdbc = jdbc; this.props = props; }

    public String issueRefresh(String subjectType, long subjectId, String ip, String ua) {
        String raw = Hashing.randomToken();
        jdbc.update("INSERT INTO refresh_tokens(subject_type,subject_id,token_hash,expires_at,user_agent,ip_address) VALUES (?,?,?,?,?,?)",
                subjectType, subjectId, Hashing.sha256(raw), Timestamp.from(Instant.now().plus(props.jwt().refreshTtlDays(), ChronoUnit.DAYS)),
                ua == null ? null : ua.substring(0, Math.min(300, ua.length())), ip);
        return raw;
    }

    /** Consumes (revokes) a valid refresh token and returns its subject, or null when invalid. */
    public Map<String, Object> consume(String raw, String subjectType) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT id, subject_id FROM refresh_tokens WHERE token_hash=? AND subject_type=? AND revoked_at IS NULL AND expires_at > UTC_TIMESTAMP()",
                Hashing.sha256(raw), subjectType);
        if (rows.isEmpty()) return null;
        jdbc.update("UPDATE refresh_tokens SET revoked_at=UTC_TIMESTAMP() WHERE id=?", rows.get(0).get("id"));
        return rows.get(0);
    }

    public void revoke(String raw) {
        jdbc.update("UPDATE refresh_tokens SET revoked_at=UTC_TIMESTAMP() WHERE token_hash=? AND revoked_at IS NULL", Hashing.sha256(raw));
    }

    public void revokeAll(String subjectType, long subjectId) {
        jdbc.update("UPDATE refresh_tokens SET revoked_at=UTC_TIMESTAMP() WHERE subject_type=? AND subject_id=? AND revoked_at IS NULL", subjectType, subjectId);
    }
}
