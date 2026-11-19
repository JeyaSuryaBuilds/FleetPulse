package com.fayzo.fleetpulse.admin;

import com.fayzo.fleetpulse.audit.AuditService;
import com.fayzo.fleetpulse.common.ApiException;
import com.fayzo.fleetpulse.security.AuthPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;


/** System monitoring, feature flags, maintenance mode, platform settings, announcements and support tickets. */
@org.springframework.validation.annotation.Validated
@RestController
@RequestMapping("/api/v1/admin")
public class AdminPlatformController {
    public record FlagInput(@NotBlank @Pattern(regexp = "^[a-z0-9_]{2,80}$") String flagKey, @Size(max = 300) String description, boolean enabled) {}
    public record Maintenance(boolean enabled, @Size(max = 500) String message) {}
    public record AnnouncementInput(@NotBlank @Size(max = 200) String title, @NotBlank String body, boolean published) {}
    public record TicketUpdate(@Pattern(regexp = "OPEN|IN_PROGRESS|RESOLVED|CLOSED") String status, @Pattern(regexp = "LOW|NORMAL|HIGH|URGENT") String priority) {}

    // Secrets are configured through environment variables only; these prefixes may be edited here.
    private static final java.util.regex.Pattern SETTING_KEY =
        java.util.regex.Pattern.compile("^(general|email|sms|payment|branding)\\.[a-z_]{2,60}$");

    private static final java.util.regex.Pattern SECRET_LIKE =
        java.util.regex.Pattern.compile(".*(secret|token|password|api_key|apikey|private).*");

    private final JdbcTemplate jdbc;
    private final AuditService audit;
    public AdminPlatformController(JdbcTemplate jdbc, AuditService audit) { this.jdbc = jdbc; this.audit = audit; }

    @GetMapping("/platform/health")
    public Map<String, Object> health() {
        Map<String, Object> o = new LinkedHashMap<>();
        long t = System.nanoTime();
        jdbc.queryForObject("SELECT 1", Integer.class);
        o.put("databaseOk", true);
        o.put("databaseLatencyMs", (System.nanoTime() - t) / 1_000_000);
        Runtime rt = Runtime.getRuntime();
        o.put("jvmUsedMb", (rt.totalMemory() - rt.freeMemory()) / 1_048_576);
        o.put("jvmMaxMb", rt.maxMemory() / 1_048_576);
        o.put("processors", rt.availableProcessors());
        Map<String, Object> db = new LinkedHashMap<>();
        db.put("gpsRows", jdbc.queryForObject("SELECT COUNT(*) FROM gps_locations", Long.class));
        db.put("auditRows", jdbc.queryForObject("SELECT COUNT(*) FROM audit_logs", Long.class));
        db.put("sizeMb", jdbc.queryForObject("SELECT COALESCE(ROUND(SUM(data_length+index_length)/1048576,1),0) FROM information_schema.tables WHERE table_schema=DATABASE()", Double.class));
        o.put("database", db);
        return o;
    }

    @GetMapping("/platform/maintenance")
    public Map<String, Object> maintenance() {
        return Map.of("enabled", "true".equals(get("maintenance_mode")), "message", Optional.ofNullable(get("maintenance_message")).orElse(""));
    }

    @PutMapping("/platform/maintenance")
    public Map<String, Boolean> setMaintenance(@Valid @RequestBody Maintenance m, @AuthenticationPrincipal AuthPrincipal a, HttpServletRequest h) {
        put("maintenance_mode", String.valueOf(m.enabled()));
        if (m.message() != null) put("maintenance_message", m.message());
        audit.log(null, "ADMIN", a.subjectId(), "MAINTENANCE_MODE_" + m.enabled(), null, null, h.getRemoteAddr());
        return Map.of("ok", true);
    }

    @GetMapping("/platform/flags")
    public List<Map<String, Object>> flags() { return jdbc.queryForList("SELECT id, flag_key AS flagKey, description, enabled FROM feature_flags ORDER BY flag_key"); }

    @PutMapping("/platform/flags")
    public Map<String, Boolean> upsertFlag(@Valid @RequestBody FlagInput f) {
        jdbc.update("INSERT INTO feature_flags(flag_key,description,enabled) VALUES (?,?,?) ON DUPLICATE KEY UPDATE description=VALUES(description), enabled=VALUES(enabled)", f.flagKey(), f.description(), f.enabled());
        return Map.of("ok", true);
    }

    @GetMapping("/settings")
    public Map<String, String> settings() {
        Map<String, String> out = new TreeMap<>();
        jdbc.query("SELECT setting_key k, setting_value v FROM platform_settings", rs -> { out.put(rs.getString("k"), rs.getString("v")); });
        return out;
    }

    @PutMapping("/settings")
    public Map<String, Boolean> updateSettings(@RequestBody Map<String, String> values, @AuthenticationPrincipal AuthPrincipal a, HttpServletRequest h) {
        for (var e : values.entrySet()) {
            if (!SETTING_KEY.matcher(e.getKey()).matches() || SECRET_LIKE.matcher(e.getKey()).matches())
                throw ApiException.badRequest("BAD_SETTING", "Setting '" + e.getKey() + "' cannot be edited here. Secrets belong in server environment variables.");
            if (e.getValue() != null && e.getValue().length() > 2000) throw ApiException.badRequest("BAD_SETTING", "Value too long");
        }
        values.forEach(this::put);
        audit.log(null, "ADMIN", a.subjectId(), "SETTINGS_UPDATED", null, String.join(",", values.keySet()), h.getRemoteAddr());
        return Map.of("ok", true);
    }

    // ---- Customer success ----
    @GetMapping("/support/tickets")
    public Map<String, Object> tickets(@RequestParam(required = false) String category, @RequestParam(required = false) String status,
                                       @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "25") int size) {
        int sz = Page.size(size), pg = Math.max(page, 0);
        StringBuilder w = new StringBuilder(" WHERE 1=1");
        List<Object> a = new ArrayList<>();
        if (category != null && !category.isBlank()) { w.append(" AND t.category=?"); a.add(category); }
        if (status != null && !status.isBlank()) { w.append(" AND t.status=?"); a.add(status); }
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM support_tickets t" + w, Long.class, a.toArray());
        a.add(sz); a.add(pg * sz);
        return Page.of(jdbc.queryForList("SELECT t.id, c.name AS company, t.category, t.subject, t.description, t.status, t.priority, t.created_at AS createdAt FROM support_tickets t JOIN companies c ON c.id=t.company_id" + w + " ORDER BY t.id DESC LIMIT ? OFFSET ?", a.toArray()), pg, sz, total == null ? 0 : total);
    }

    @PatchMapping("/support/tickets/{id}")
    public Map<String, Boolean> updateTicket(@PathVariable long id, @Valid @RequestBody TicketUpdate u) {
        if (u.status() != null) jdbc.update("UPDATE support_tickets SET status=? WHERE id=?", u.status(), id);
        if (u.priority() != null) jdbc.update("UPDATE support_tickets SET priority=? WHERE id=?", u.priority(), id);
        return Map.of("ok", true);
    }

    @GetMapping("/contact-requests")
    public List<Map<String, Object>> contactRequests(@RequestParam(required = false) String status) {
        if (status == null || status.isBlank()) return jdbc.queryForList("SELECT id, kind, name, email, mobile, company_name AS companyName, message, status, created_at AS createdAt FROM contact_requests ORDER BY id DESC LIMIT 200");
        return jdbc.queryForList("SELECT id, kind, name, email, mobile, company_name AS companyName, message, status, created_at AS createdAt FROM contact_requests WHERE status=? ORDER BY id DESC LIMIT 200", status);
    }

    @PatchMapping("/contact-requests/{id}")
    public Map<String, Boolean> updateContact(@PathVariable long id, @RequestParam @Pattern(regexp = "NEW|CONTACTED|CLOSED") String status) {
        jdbc.update("UPDATE contact_requests SET status=? WHERE id=?", status, id);
        return Map.of("ok", true);
    }

    @GetMapping("/announcements")
    public List<Map<String, Object>> announcements() { return jdbc.queryForList("SELECT id, title, body, published, created_at AS createdAt FROM announcements ORDER BY id DESC LIMIT 100"); }

    @PostMapping("/announcements")
    public Map<String, Boolean> addAnnouncement(@Valid @RequestBody AnnouncementInput a) {
        jdbc.update("INSERT INTO announcements(title,body,published) VALUES (?,?,?)", a.title(), a.body(), a.published());
        return Map.of("ok", true);
    }

    @PutMapping("/announcements/{id}")
    public Map<String, Boolean> updateAnnouncement(@PathVariable long id, @Valid @RequestBody AnnouncementInput a) {
        if (jdbc.update("UPDATE announcements SET title=?, body=?, published=? WHERE id=?", a.title(), a.body(), a.published(), id) == 0) throw ApiException.notFound("Announcement not found");
        return Map.of("ok", true);
    }

    private String get(String k) {
        List<String> r = jdbc.queryForList("SELECT setting_value FROM platform_settings WHERE setting_key=?", String.class, k);
        return r.isEmpty() ? null : r.get(0);
    }
    private void put(String k, String v) {
        jdbc.update("INSERT INTO platform_settings(setting_key,setting_value) VALUES (?,?) ON DUPLICATE KEY UPDATE setting_value=VALUES(setting_value)", k, v);
    }
}
