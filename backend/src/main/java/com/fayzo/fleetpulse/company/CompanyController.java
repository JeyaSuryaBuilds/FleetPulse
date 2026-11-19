package com.fayzo.fleetpulse.company;

import com.fayzo.fleetpulse.common.ApiException;
import com.fayzo.fleetpulse.common.Authz;
import com.fayzo.fleetpulse.security.Permission;
import com.fayzo.fleetpulse.subscription.PlanLimits;
import com.fayzo.fleetpulse.tenant.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/company")
public class CompanyController {
    public record ProfileUpdate(@Size(min = 2, max = 160) String name, @Size(max = 500) String address, @Size(max = 80) String country,
                                @Size(max = 60) String timezone, @Size(min = 3, max = 3) String currency, @Size(max = 500) String logoUrl) {}

    private final JdbcTemplate jdbc;
    private final PlanLimits limits;
    private final com.fayzo.fleetpulse.billing.StripeBilling stripe;
    public CompanyController(JdbcTemplate jdbc, PlanLimits limits, com.fayzo.fleetpulse.billing.StripeBilling stripe) { this.jdbc = jdbc; this.limits = limits; this.stripe = stripe; }

    @GetMapping
    public Map<String, Object> profile() {
        Authz.require(Permission.DASHBOARD_VIEW);
        return jdbc.queryForMap("SELECT id,name,email,mobile,status,logo_url AS logoUrl,address,country,timezone,currency,created_at AS createdAt FROM companies WHERE id=?", TenantContext.require());
    }

    @PutMapping
    public Map<String, Object> update(@Valid @RequestBody ProfileUpdate u) {
        Authz.require(Permission.ORG_MANAGE);
        long c = TenantContext.require();
        jdbc.update("""
            UPDATE companies SET name=COALESCE(?,name), address=COALESCE(?,address), country=COALESCE(?,country),
              timezone=COALESCE(?,timezone), currency=COALESCE(?,currency), logo_url=COALESCE(?,logo_url) WHERE id=?""",
                u.name(), u.address(), u.country(), u.timezone(), u.currency(), u.logoUrl(), c);
        return profile();
    }

    /** Current plan, trial/renewal dates and live usage against plan limits. */
    @GetMapping("/subscription")
    public Map<String, Object> subscription() {
        Authz.require(Permission.SUBSCRIPTION_MANAGE);
        long c = TenantContext.require();
        Map<String, Object> plan = limits.currentPlan(c);
        Map<String, Object> usage = new LinkedHashMap<>();
        usage.put("vehicles", jdbc.queryForObject("SELECT COUNT(*) FROM vehicles WHERE company_id=? AND status NOT IN ('SOLD','RETIRED')", Long.class, c));
        usage.put("users", jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE company_id=? AND status='ACTIVE'", Long.class, c));
        usage.put("aiRequestsThisMonth", jdbc.queryForObject("SELECT COUNT(*) FROM ai_usage WHERE company_id=? AND created_at >= DATE_FORMAT(UTC_TIMESTAMP(),'%Y-%m-01')", Long.class, c));
        usage.put("gpsPointsThisMonth", jdbc.queryForObject("SELECT COUNT(*) FROM gps_locations WHERE company_id=? AND received_at >= DATE_FORMAT(UTC_TIMESTAMP(),'%Y-%m-01')", Long.class, c));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("plan", plan);
        out.put("usage", usage);
        out.put("invoices", jdbc.queryForList("SELECT invoice_number AS invoiceNumber, total, currency, status, issued_at AS issuedAt, paid_at AS paidAt FROM platform_invoices WHERE company_id=? ORDER BY id DESC LIMIT 50", c));
        return out;
    }

    @GetMapping("/announcements")
    public java.util.List<Map<String, Object>> announcements() {
        Authz.require(Permission.NOTIFICATION_READ);
        return jdbc.queryForList("SELECT id, title, body, created_at AS createdAt FROM announcements WHERE published=TRUE AND (starts_at IS NULL OR starts_at <= UTC_TIMESTAMP()) AND (ends_at IS NULL OR ends_at >= UTC_TIMESTAMP()) ORDER BY id DESC LIMIT 20");
    }

    @PostMapping("/subscription/cancel")
    public Map<String, Boolean> cancel() {
        Authz.require(Permission.SUBSCRIPTION_MANAGE);
        java.util.List<String> ref = jdbc.queryForList("SELECT gateway_subscription_ref FROM subscriptions WHERE company_id=? AND status IN ('TRIALING','ACTIVE') AND gateway_subscription_ref IS NOT NULL ORDER BY id DESC LIMIT 1", String.class, TenantContext.require());
        if (!ref.isEmpty()) stripe.cancelAtPeriodEnd(ref.get(0));   // paid subscription: tell the payment provider first
        int n = jdbc.update("UPDATE subscriptions SET cancel_at_period_end=TRUE, cancelled_at=UTC_TIMESTAMP() WHERE company_id=? AND status IN ('TRIALING','ACTIVE')", TenantContext.require());
        if (n == 0) throw ApiException.badRequest("NOTHING_TO_CANCEL", "No active subscription to cancel");
        return Map.of("ok", true);
    }
}
