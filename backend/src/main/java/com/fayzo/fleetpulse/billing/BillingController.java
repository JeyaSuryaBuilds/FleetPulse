package com.fayzo.fleetpulse.billing;

import com.fayzo.fleetpulse.common.ApiException;
import com.fayzo.fleetpulse.common.Authz;
import com.fayzo.fleetpulse.security.Permission;
import com.fayzo.fleetpulse.tenant.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Company-side billing: start checkout for a plan (upgrade, downgrade, renewal after expiry) and cancel at period end. */
@RestController
@RequestMapping("/api/v1/billing")
public class BillingController {
    public record CheckoutRequest(@NotBlank String planCode, @NotBlank @Pattern(regexp = "MONTHLY|ANNUAL") String billingCycle) {}
    private final StripeBilling stripe;
    private final JdbcTemplate jdbc;
    private final String publicUrl;

    public BillingController(StripeBilling stripe, JdbcTemplate jdbc, @Value("${fleetpulse.public-url}") String publicUrl) { this.stripe = stripe; this.jdbc = jdbc; this.publicUrl = publicUrl; }

    @GetMapping("/status")
    public Map<String, Object> status() { Authz.require(Permission.SUBSCRIPTION_MANAGE); return Map.of("paymentsConfigured", stripe.configured()); }

    /** The price always comes from the plans table, never from the request. */
    @PostMapping("/checkout")
    public Map<String, String> checkout(@Valid @RequestBody CheckoutRequest r) {
        Authz.require(Permission.SUBSCRIPTION_MANAGE);
        long c = TenantContext.require();
        List<Map<String, Object>> p = jdbc.queryForList("SELECT code,name,monthly_price,annual_price,currency FROM plans WHERE code=? AND active=TRUE", r.planCode());
        if (p.isEmpty()) throw ApiException.badRequest("UNKNOWN_PLAN", "Plan not available");
        boolean annual = "ANNUAL".equals(r.billingCycle());
        BigDecimal price = (BigDecimal) p.get(0).get(annual ? "annual_price" : "monthly_price");
        if (price.signum() <= 0) throw ApiException.badRequest("NOT_PAYABLE", "This plan has no price configured");
        String email = jdbc.queryForObject("SELECT email FROM companies WHERE id=?", String.class, c);
        String url = stripe.createCheckoutUrl(c, email, r.planCode(), (String) p.get(0).get("name"), price, (String) p.get(0).get("currency"), annual,
                publicUrl + "/app/subscription?checkout=success", publicUrl + "/app/subscription?checkout=cancelled");
        return Map.of("checkoutUrl", url);
    }
}
