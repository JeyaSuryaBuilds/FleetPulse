package com.fayzo.fleetpulse.admin;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/** Platform-wide analytics for the Fayzo Super Admin. Access is restricted to ROLE_SUPER_ADMIN by SecurityConfig. */
@RestController
@RequestMapping("/api/v1/admin/dashboard")
public class AdminDashboardController {
    private final JdbcTemplate jdbc;
    public AdminDashboardController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    private long n(String sql, Object... a) { Long v = jdbc.queryForObject(sql, Long.class, a); return v == null ? 0 : v; }
    private BigDecimal d(String sql, Object... a) { BigDecimal v = jdbc.queryForObject(sql, BigDecimal.class, a); return v == null ? BigDecimal.ZERO : v; }

    @GetMapping
    public Map<String, Object> stats() {
        Map<String, Object> o = new LinkedHashMap<>();
        o.put("totalCompanies", n("SELECT COUNT(*) FROM companies"));
        o.put("activeCompanies", n("SELECT COUNT(*) FROM companies WHERE status='ACTIVE'"));
        o.put("trialCompanies", n("SELECT COUNT(*) FROM companies WHERE status='TRIAL'"));
        o.put("suspendedCompanies", n("SELECT COUNT(*) FROM companies WHERE status='SUSPENDED'"));
        o.put("expiredCompanies", n("SELECT COUNT(*) FROM companies WHERE status='EXPIRED'"));
        o.put("cancelledCompanies", n("SELECT COUNT(*) FROM companies WHERE status='CANCELLED'"));
        o.put("totalUsers", n("SELECT COUNT(*) FROM users"));
        o.put("totalVehicles", n("SELECT COUNT(*) FROM vehicles"));
        // MRR counts only paying (ACTIVE) subscriptions; trials contribute nothing until they convert.
        BigDecimal mrr = d("""
            SELECT SUM(CASE WHEN s.billing_cycle='ANNUAL' THEN p.annual_price/12 ELSE p.monthly_price END)
            FROM subscriptions s JOIN plans p ON p.id=s.plan_id WHERE s.status='ACTIVE'""");
        o.put("mrr", mrr);
        o.put("arr", mrr.multiply(BigDecimal.valueOf(12)));
        o.put("subscriptionRevenueTotal", d("SELECT SUM(amount) FROM platform_payments WHERE status='SUCCEEDED'"));
        o.put("subscriptionRevenueThisMonth", d("SELECT SUM(amount) FROM platform_payments WHERE status='SUCCEEDED' AND created_at >= DATE_FORMAT(UTC_TIMESTAMP(),'%Y-%m-01')"));
        o.put("aiRequestsThisMonth", n("SELECT COUNT(*) FROM ai_usage WHERE created_at >= DATE_FORMAT(UTC_TIMESTAMP(),'%Y-%m-01')"));
        o.put("aiTokensThisMonth", n("SELECT COALESCE(SUM(prompt_tokens+completion_tokens),0) FROM ai_usage WHERE created_at >= DATE_FORMAT(UTC_TIMESTAMP(),'%Y-%m-01')"));
        o.put("signupsPerDay", jdbc.queryForList("SELECT DATE(created_at) AS day, COUNT(*) AS count FROM companies WHERE created_at >= DATE_SUB(UTC_TIMESTAMP(), INTERVAL 30 DAY) GROUP BY DATE(created_at) ORDER BY day"));
        o.put("companiesByPlan", jdbc.queryForList("SELECT p.name AS plan, COUNT(*) AS count FROM subscriptions s JOIN plans p ON p.id=s.plan_id WHERE s.status IN ('TRIALING','ACTIVE') GROUP BY p.name"));
        return o;
    }
}
