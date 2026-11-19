package com.fayzo.fleetpulse.security;

import com.fayzo.fleetpulse.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtService {
    private final AppProperties props;
    private final SecretKey key;

    public JwtService(AppProperties props) {
        this.props = props;
        byte[] raw = props.jwt().secret().getBytes(StandardCharsets.UTF_8);
        if (raw.length < 32) throw new IllegalStateException("FP_JWT_SECRET must be at least 32 bytes");
        this.key = Keys.hmacShaKeyFor(raw);
    }

    public String issue(AuthPrincipal p, int ttlMinutes) {
        Instant now = Instant.now();
        var b = Jwts.builder()
                .issuer(props.jwt().issuer())
                .subject(p.type() + ":" + p.subjectId())
                .claim("typ", p.type())
                .claim("role", p.role())
                .claim("tv", p.tokenVersion())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttlMinutes * 60L)));
        if (p.companyId() != null) b.claim("cid", p.companyId());
        return b.signWith(key).compact();
    }

    public String issueUserAccess(AuthPrincipal p) { return issue(p, props.jwt().accessTtlMinutes()); }
    public String issueAdminAccess(AuthPrincipal p) { return issue(p, props.superadmin().sessionTtlMinutes()); }

    public Optional<AuthPrincipal> parse(String token) {
        try {
            Claims c = Jwts.parser().verifyWith(key).requireIssuer(props.jwt().issuer()).build().parseSignedClaims(token).getPayload();
            String typ = c.get("typ", String.class);
            long id = Long.parseLong(c.getSubject().substring(c.getSubject().indexOf(':') + 1));
            Number cid = c.get("cid", Number.class);
            return Optional.of(new AuthPrincipal(typ, id, cid == null ? null : cid.longValue(),
                    c.get("role", String.class), c.get("tv", Integer.class) == null ? 0 : c.get("tv", Integer.class)));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
