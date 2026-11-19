package com.fayzo.fleetpulse.security;

import com.fayzo.fleetpulse.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates the bearer token, re-checks token_version and account/company status in the database
 * (so disabling a user or suspending a company takes effect immediately), and sets the tenant context.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final JdbcTemplate jdbc;

    public JwtAuthFilter(JwtService jwt, JdbcTemplate jdbc) { this.jwt = jwt; this.jdbc = jdbc; }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        try {
            String h = req.getHeader("Authorization");
            if (h != null && h.startsWith("Bearer ")) {
                jwt.parse(h.substring(7)).ifPresent(p -> authenticate(p));
            }
            chain.doFilter(req, res);
        } finally {
            TenantContext.clear();
            SecurityContextHolder.clearContext();
        }
    }

    private void authenticate(AuthPrincipal p) {
        List<GrantedAuthority> auth = new ArrayList<>();
        if (p.isAdmin()) {
            Integer tv = single("SELECT token_version FROM platform_admins WHERE id=?", p.subjectId());
            if (tv == null || tv != p.tokenVersion()) return;
            auth.add(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
        } else {
            var rows = jdbc.queryForList("""
                SELECT u.token_version tv, u.status ustatus, u.role_code role, c.status cstatus
                FROM users u JOIN companies c ON c.id=u.company_id WHERE u.id=? AND u.company_id=?""", p.subjectId(), p.companyId());
            if (rows.isEmpty()) return;
            var r = rows.get(0);
            if (((Number) r.get("tv")).intValue() != p.tokenVersion()) return;
            if (!"ACTIVE".equals(r.get("ustatus"))) return;
            String cstatus = (String) r.get("cstatus");
            if ("SUSPENDED".equals(cstatus) || "CANCELLED".equals(cstatus)) return;
            Role role;
            try { role = Role.valueOf((String) r.get("role")); } catch (IllegalArgumentException e) { return; }
            auth.add(new SimpleGrantedAuthority("ROLE_" + role.name()));
            role.permissions().forEach(perm -> auth.add(new SimpleGrantedAuthority(perm.name())));
            TenantContext.set(p.companyId());
        }
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(p, null, auth));
    }

    private Integer single(String sql, Object... args) {
        var l = jdbc.queryForList(sql, Integer.class, args);
        return l.isEmpty() ? null : l.get(0);
    }
}
