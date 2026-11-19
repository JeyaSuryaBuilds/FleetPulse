package com.fayzo.fleetpulse.platform;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/** When maintenance mode is on, tenant and public APIs return 503. The Super Admin API stays available so it can be switched off. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class MaintenanceModeFilter extends OncePerRequestFilter {
    private final JdbcTemplate jdbc;
    private volatile long checkedAt = 0;
    private volatile boolean on = false;
    private volatile String message = "";

    public MaintenanceModeFilter(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException {
        String path = req.getRequestURI();
        if (path.startsWith("/api/v1/") && !path.startsWith("/api/v1/admin/")) {
            refresh();
            if (on) {
                res.setStatus(503);
                res.setContentType("application/json");
                res.getWriter().write("{\"status\":503,\"code\":\"MAINTENANCE\",\"message\":" + quote(message) + "}");
                return;
            }
        }
        chain.doFilter(req, res);
    }

    private void refresh() {
        long now = System.currentTimeMillis();
        if (now - checkedAt < 5000) return;
        checkedAt = now;
        try {
            List<String> mode = jdbc.queryForList("SELECT setting_value FROM platform_settings WHERE setting_key='maintenance_mode'", String.class);
            List<String> msg = jdbc.queryForList("SELECT setting_value FROM platform_settings WHERE setting_key='maintenance_message'", String.class);
            on = !mode.isEmpty() && "true".equals(mode.get(0));
            message = msg.isEmpty() || msg.get(0) == null ? "Maintenance in progress" : msg.get(0);
        } catch (Exception ignored) { /* keep last known state */ }
    }

    private static String quote(String s) { return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ") + "\""; }
}
