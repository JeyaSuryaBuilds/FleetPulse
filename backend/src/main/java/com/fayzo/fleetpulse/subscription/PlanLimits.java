package com.fayzo.fleetpulse.subscription;

import com.fayzo.fleetpulse.common.ApiException;
import com.fayzo.fleetpulse.tenant.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/** Enforces plan limits on the backend (never only in the UI). */
@Component
public class PlanLimits {
    private final JdbcTemplate jdbc;
    public PlanLimits(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Map<String, Object> currentPlan(long companyId) {
        List<Map<String, Object>> r = jdbc.queryForList("""
            SELECT p.code, p.name, p.vehicle_limit, p.user_limit, p.ai_monthly_quota, p.gps_monthly_quota, s.status, s.trial_ends_at, s.current_period_end
            FROM subscriptions s JOIN plans p ON p.id=s.plan_id
            WHERE s.company_id=? ORDER BY s.id DESC LIMIT 1""", companyId);
        if (r.isEmpty()) throw new ApiException(HttpStatus.PAYMENT_REQUIRED, "NO_SUBSCRIPTION", "No active subscription");
        return r.get(0);
    }

    private void requireActive(Map<String, Object> plan) {
        String st = (String) plan.get("status");
        if ("EXPIRED".equals(st) || "CANCELLED".equals(st))
            throw new ApiException(HttpStatus.PAYMENT_REQUIRED, "SUBSCRIPTION_INACTIVE", "Your subscription is " + st.toLowerCase() + ". Please renew to add records.");
    }

    public void requireVehicleCapacity() {
        long c = TenantContext.require();
        Map<String, Object> plan = currentPlan(c);
        requireActive(plan);
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM vehicles WHERE company_id=? AND status<>'SOLD' AND status<>'RETIRED'", Integer.class, c);
        if (n != null && n >= ((Number) plan.get("vehicle_limit")).intValue())
            throw new ApiException(HttpStatus.PAYMENT_REQUIRED, "VEHICLE_LIMIT", "Vehicle limit of your plan reached. Upgrade to add more vehicles.");
    }

    public void requireUserCapacity() {
        long c = TenantContext.require();
        Map<String, Object> plan = currentPlan(c);
        requireActive(plan);
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE company_id=? AND status='ACTIVE'", Integer.class, c);
        if (n != null && n >= ((Number) plan.get("user_limit")).intValue())
            throw new ApiException(HttpStatus.PAYMENT_REQUIRED, "USER_LIMIT", "User limit of your plan reached. Upgrade to add more users.");
    }
}
