package com.fayzo.fleetpulse.gps;

import com.fayzo.fleetpulse.common.ApiException;
import com.fayzo.fleetpulse.subscription.PlanLimits;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/** Enforces the plan's monthly GPS point quota. The month's count is cached for 60 seconds per company (approximate near the limit). */
@Component
public class GpsQuota {
    private record Entry(long count, long at) {}
    private final ConcurrentHashMap<Long, Entry> cache = new ConcurrentHashMap<>();
    private final JdbcTemplate jdbc;
    private final PlanLimits plans;
    public GpsQuota(JdbcTemplate jdbc, PlanLimits plans) { this.jdbc = jdbc; this.plans = plans; }

    public void check(long companyId) {
        long quota = ((Number) plans.currentPlan(companyId).get("gps_monthly_quota")).longValue();
        Entry e = cache.get(companyId);
        long now = System.currentTimeMillis();
        if (e == null || now - e.at() > 60_000) {
            Long c = jdbc.queryForObject("SELECT COUNT(*) FROM gps_locations WHERE company_id=? AND received_at >= DATE_FORMAT(UTC_TIMESTAMP(),'%Y-%m-01')", Long.class, companyId);
            e = new Entry(c == null ? 0 : c, now);
        }
        if (e.count() >= quota) throw new ApiException(HttpStatus.PAYMENT_REQUIRED, "GPS_QUOTA", "Monthly GPS quota of your plan is used up");
        cache.put(companyId, new Entry(e.count() + 1, e.at()));
    }
}
