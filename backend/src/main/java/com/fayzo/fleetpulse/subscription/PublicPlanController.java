package com.fayzo.fleetpulse.subscription;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Pricing shown on the public website. Values come from the plans table, managed by the Super Admin. */
@RestController
@RequestMapping("/api/v1/public/plans")
public class PublicPlanController {
    private final JdbcTemplate jdbc;
    public PublicPlanController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @GetMapping
    public List<Map<String, Object>> plans() {
        return jdbc.queryForList("""
            SELECT code, name, description, monthly_price AS monthlyPrice, annual_price AS annualPrice, currency,
                   trial_days AS trialDays, vehicle_limit AS vehicleLimit, user_limit AS userLimit,
                   ai_monthly_quota AS aiMonthlyQuota, gps_monthly_quota AS gpsMonthlyQuota
            FROM plans WHERE active=TRUE ORDER BY sort_order""");
    }
}
