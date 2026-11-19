package com.fayzo.fleetpulse.admin;

import com.fayzo.fleetpulse.audit.AuditService;
import com.fayzo.fleetpulse.common.ApiException;
import com.fayzo.fleetpulse.security.AuthPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/** Company directory and lifecycle. There is NO approval workflow: companies are active from registration. */
@RestController
@RequestMapping("/api/v1/admin/companies")
@Transactional
public class AdminCompanyController {
    public record StatusChange(@NotBlank @Pattern(regexp = "ACTIVE|SUSPENDED|CANCELLED|EXPIRED|TRIAL") String status, @Size(max = 300) String reason) {}
    public record SubscriptionChange(@Size(max = 40) String planCode, @Pattern(regexp = "MONTHLY|ANNUAL") String billingCycle,
                                     @Min(0) @Max(365) Integer extendTrialDays, @Pattern(regexp = "TRIALING|ACTIVE|PAST_DUE|CANCELLED|EXPIRED") String status) {}

    private final JdbcTemplate jdbc;
    private final AuditService audit;
    public AdminCompanyController(JdbcTemplate jdbc, AuditService audit) { this.jdbc = jdbc; this.audit = audit; }

    @GetMapping
    @Transactional(readOnly = true)
    public Map<String, Object> list(@RequestParam(required = false) String status, @RequestParam(required = false) String q,
                                    @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "25") int size) {
        int sz = Page.size(size), pg = Math.max(page, 0);
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        if (status != null && !status.isBlank()) { where.append(" AND c.status=?"); args.add(status); }
        if (q != null && !q.isBlank()) { where.append(" AND (c.name LIKE ? OR c.email LIKE ? OR c.mobile LIKE ?)"); String l = "%" + q.replace("%", "") + "%"; args.add(l); args.add(l); args.add(l); }
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM companies c" + where, Long.class, args.toArray());
        List<Object> a2 = new ArrayList<>(args); a2.add(sz); a2.add(pg * sz);
        List<Map<String, Object>> rows = jdbc.queryForList("""
            SELECT c.id, c.name, c.email, c.mobile, c.status, c.created_at AS createdAt,
              (SELECT p.name FROM subscriptions s JOIN plans p ON p.id=s.plan_id WHERE s.company_id=c.id ORDER BY s.id DESC LIMIT 1) AS plan,
              (SELECT COUNT(*) FROM users u WHERE u.company_id=c.id) AS users,
              (SELECT COUNT(*) FROM vehicles v WHERE v.company_id=c.id) AS vehicles
            FROM companies c""" + where + " ORDER BY c.id DESC LIMIT ? OFFSET ?", a2.toArray());
        return Page.of(rows, pg, sz, total == null ? 0 : total);
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public Map<String, Object> detail(@PathVariable long id) {
        List<Map<String, Object>> c = jdbc.queryForList("SELECT id,name,email,mobile,status,country,timezone,currency,created_at AS createdAt FROM companies WHERE id=?", id);
        if (c.isEmpty()) throw ApiException.notFound("Company not found");
        Map<String, Object> o = new LinkedHashMap<>(c.get(0));
        o.put("subscription", jdbc.queryForList("""
            SELECT s.id, p.code AS planCode, p.name AS plan, s.status, s.billing_cycle AS billingCycle, s.trial_ends_at AS trialEndsAt,
                   s.current_period_end AS currentPeriodEnd, s.cancel_at_period_end AS cancelAtPeriodEnd
            FROM subscriptions s JOIN plans p ON p.id=s.plan_id WHERE s.company_id=? ORDER BY s.id DESC LIMIT 1""", id));
        Map<String, Object> usage = new LinkedHashMap<>();
        usage.put("users", jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE company_id=?", Long.class, id));
        usage.put("vehicles", jdbc.queryForObject("SELECT COUNT(*) FROM vehicles WHERE company_id=?", Long.class, id));
        usage.put("drivers", jdbc.queryForObject("SELECT COUNT(*) FROM drivers WHERE company_id=?", Long.class, id));
        usage.put("shipments", jdbc.queryForObject("SELECT COUNT(*) FROM shipments WHERE company_id=?", Long.class, id));
        usage.put("aiRequestsThisMonth", jdbc.queryForObject("SELECT COUNT(*) FROM ai_usage WHERE company_id=? AND created_at >= DATE_FORMAT(UTC_TIMESTAMP(),'%Y-%m-01')", Long.class, id));
        usage.put("gpsPointsThisMonth", jdbc.queryForObject("SELECT COUNT(*) FROM gps_locations WHERE company_id=? AND received_at >= DATE_FORMAT(UTC_TIMESTAMP(),'%Y-%m-01')", Long.class, id));
        o.put("usage", usage);
        o.put("recentActivity", jdbc.queryForList("SELECT action, actor_type AS actorType, created_at AS createdAt FROM audit_logs WHERE company_id=? ORDER BY id DESC LIMIT 25", id));
        return o;
    }

    @PatchMapping("/{id}/status")
    public Map<String, Boolean> setStatus(@PathVariable long id, @Valid @RequestBody StatusChange r, @AuthenticationPrincipal AuthPrincipal admin, HttpServletRequest http) {
        if (jdbc.update("UPDATE companies SET status=? WHERE id=?", r.status(), id) == 0) throw ApiException.notFound("Company not found");
        // Users of a suspended company are rejected on their next request (JwtAuthFilter re-checks company status).
        audit.log(id, "ADMIN", admin.subjectId(), "COMPANY_STATUS_" + r.status(), "company", String.valueOf(id), http.getRemoteAddr());
        return Map.of("ok", true);
    }

    @PutMapping("/{id}/subscription")
    public Map<String, Boolean> changeSubscription(@PathVariable long id, @Valid @RequestBody SubscriptionChange r, @AuthenticationPrincipal AuthPrincipal admin, HttpServletRequest http) {
        List<Map<String, Object>> sub = jdbc.queryForList("SELECT id FROM subscriptions WHERE company_id=? ORDER BY id DESC LIMIT 1", id);
        if (sub.isEmpty()) throw ApiException.notFound("Subscription not found");
        long sid = ((Number) sub.get(0).get("id")).longValue();
        if (r.planCode() != null) {
            List<Long> p = jdbc.queryForList("SELECT id FROM plans WHERE code=?", Long.class, r.planCode());
            if (p.isEmpty()) throw ApiException.badRequest("UNKNOWN_PLAN", "Unknown plan");
            jdbc.update("UPDATE subscriptions SET plan_id=? WHERE id=?", p.get(0), sid);
        }
        if (r.billingCycle() != null) jdbc.update("UPDATE subscriptions SET billing_cycle=? WHERE id=?", r.billingCycle(), sid);
        if (r.extendTrialDays() != null && r.extendTrialDays() > 0)
            jdbc.update("UPDATE subscriptions SET trial_ends_at=DATE_ADD(COALESCE(trial_ends_at, UTC_TIMESTAMP()), INTERVAL ? DAY), current_period_end=DATE_ADD(current_period_end, INTERVAL ? DAY) WHERE id=?", r.extendTrialDays(), r.extendTrialDays(), sid);
        if (r.status() != null) jdbc.update("UPDATE subscriptions SET status=? WHERE id=?", r.status(), sid);
        audit.log(id, "ADMIN", admin.subjectId(), "SUBSCRIPTION_CHANGED", "subscription", String.valueOf(sid), http.getRemoteAddr());
        return Map.of("ok", true);
    }
}
