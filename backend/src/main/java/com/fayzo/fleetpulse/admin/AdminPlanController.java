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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Plan pricing, limits and quotas. The public pricing page reads from the same table. */
@RestController
@RequestMapping("/api/v1/admin/plans")
public class AdminPlanController {
    public record PlanInput(@NotBlank @Pattern(regexp = "^[A-Z0-9_]{2,40}$") String code, @NotBlank @Size(max = 80) String name, @Size(max = 500) String description,
                            @NotNull @DecimalMin("0") BigDecimal monthlyPrice, @NotNull @DecimalMin("0") BigDecimal annualPrice,
                            @Size(min = 3, max = 3) String currency, @Min(0) @Max(365) int trialDays, @Min(1) int vehicleLimit, @Min(1) int userLimit,
                            @Min(0) int aiMonthlyQuota, @Min(0) long gpsMonthlyQuota, String featuresJson, boolean active, int sortOrder) {}

    private final JdbcTemplate jdbc;
    private final AuditService audit;
    public AdminPlanController(JdbcTemplate jdbc, AuditService audit) { this.jdbc = jdbc; this.audit = audit; }

    @GetMapping
    public List<Map<String, Object>> list() {
        return jdbc.queryForList("SELECT * FROM plans ORDER BY sort_order, id");
    }

    @PostMapping
    public Map<String, Boolean> create(@Valid @RequestBody PlanInput p, @AuthenticationPrincipal AuthPrincipal a, HttpServletRequest h) {
        try {
            jdbc.update("""
                INSERT INTO plans(code,name,description,monthly_price,annual_price,currency,trial_days,vehicle_limit,user_limit,ai_monthly_quota,gps_monthly_quota,features_json,active,sort_order)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)""", p.code(), p.name(), p.description(), p.monthlyPrice(), p.annualPrice(), p.currency() == null ? "USD" : p.currency(),
                    p.trialDays(), p.vehicleLimit(), p.userLimit(), p.aiMonthlyQuota(), p.gpsMonthlyQuota(), p.featuresJson(), p.active(), p.sortOrder());
        } catch (org.springframework.dao.DuplicateKeyException e) { throw ApiException.conflict("PLAN_EXISTS", "A plan with this code already exists"); }
        audit.log(null, "ADMIN", a.subjectId(), "PLAN_CREATED", "plan", p.code(), h.getRemoteAddr());
        return Map.of("ok", true);
    }

    @PutMapping("/{code}")
    public Map<String, Boolean> update(@PathVariable String code, @Valid @RequestBody PlanInput p, @AuthenticationPrincipal AuthPrincipal a, HttpServletRequest h) {
        int n = jdbc.update("""
            UPDATE plans SET name=?,description=?,monthly_price=?,annual_price=?,currency=?,trial_days=?,vehicle_limit=?,user_limit=?,ai_monthly_quota=?,gps_monthly_quota=?,features_json=?,active=?,sort_order=? WHERE code=?""",
                p.name(), p.description(), p.monthlyPrice(), p.annualPrice(), p.currency() == null ? "USD" : p.currency(), p.trialDays(), p.vehicleLimit(), p.userLimit(),
                p.aiMonthlyQuota(), p.gpsMonthlyQuota(), p.featuresJson(), p.active(), p.sortOrder(), code);
        if (n == 0) throw ApiException.notFound("Plan not found");
        audit.log(null, "ADMIN", a.subjectId(), "PLAN_UPDATED", "plan", code, h.getRemoteAddr());
        return Map.of("ok", true);
    }
}
