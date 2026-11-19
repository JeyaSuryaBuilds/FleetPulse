package com.fayzo.fleetpulse.security;

import com.fayzo.fleetpulse.config.AppProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fixed-window per-IP limiter for public and auth endpoints. In-memory: for multi-instance deployments
 * replace with a shared store (Redis) or gateway-level limiting.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private record Window(long minute, int count) {}
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final AppProperties props;

    public RateLimitFilter(AppProperties props) { this.props = props; }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String path = req.getRequestURI();
        int limit;
        String bucket;
        if (path.contains("/auth/") || path.contains("/registration/")) { limit = props.rateLimit().authPerMinute(); bucket = "auth"; }
        else if (path.startsWith("/api/v1/public/")) { limit = props.rateLimit().publicPerMinute(); bucket = "public"; }
        else { chain.doFilter(req, res); return; }

        String key = bucket + ":" + req.getRemoteAddr();
        long minute = System.currentTimeMillis() / 60_000;
        Window w = windows.merge(key, new Window(minute, 1), (old, n) -> old.minute() == minute ? new Window(minute, old.count() + 1) : n);
        if (w.count() > limit) {
            res.setStatus(429);
            res.setContentType("application/json");
            res.getWriter().write("{\"status\":429,\"code\":\"RATE_LIMITED\",\"message\":\"Too many requests. Try again shortly.\"}");
            return;
        }
        if (windows.size() > 50_000) windows.entrySet().removeIf(e -> e.getValue().minute() < minute);
        chain.doFilter(req, res);
    }
}
