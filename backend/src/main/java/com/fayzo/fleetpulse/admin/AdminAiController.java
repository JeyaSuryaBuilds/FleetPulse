package com.fayzo.fleetpulse.admin;

import com.fayzo.fleetpulse.audit.AuditService;
import com.fayzo.fleetpulse.common.ApiException;
import com.fayzo.fleetpulse.config.AppProperties;
import com.fayzo.fleetpulse.security.AuthPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

/** NVIDIA NIM configuration and usage analytics. The API key itself lives only in the server environment (FP_NVIDIA_API_KEY). */
@RestController
@RequestMapping("/api/v1/admin/ai")
public class AdminAiController {
    public record ModelInput(@NotBlank @Size(max = 160) String modelId, @NotBlank @Size(max = 160) String displayName, boolean active,
                             @NotNull @DecimalMin("0") BigDecimal inputCostPer1k, @NotNull @DecimalMin("0") BigDecimal outputCostPer1k) {}
    public record AiConfig(boolean enabled, @Size(max = 160) String defaultModel, @Min(1) @Max(10000) int rateLimitPerMinute) {}

    private final JdbcTemplate jdbc;
    private final AuditService audit;
    private final AppProperties props;
    public AdminAiController(JdbcTemplate jdbc, AuditService audit, AppProperties props) { this.jdbc = jdbc; this.audit = audit; this.props = props; }

    @GetMapping("/config")
    public Map<String, Object> config() {
        Map<String, Object> o = new LinkedHashMap<>();
        o.put("enabled", "true".equals(setting("ai.enabled")));
        o.put("defaultModel", setting("ai.default_model"));
        o.put("rateLimitPerMinute", Integer.parseInt(Optional.ofNullable(setting("ai.rate_limit_per_minute")).orElse("20")));
        String key = System.getenv("FP_NVIDIA_API_KEY");
        o.put("apiKeyConfigured", key != null && !key.isBlank());   // never return the key
        return o;
    }

    @PutMapping("/config")
    public Map<String, Boolean> setConfig(@Valid @RequestBody AiConfig c, @AuthenticationPrincipal AuthPrincipal a, HttpServletRequest h) {
        put("ai.enabled", String.valueOf(c.enabled()));
        put("ai.default_model", c.defaultModel() == null ? "" : c.defaultModel());
        put("ai.rate_limit_per_minute", String.valueOf(c.rateLimitPerMinute()));
        audit.log(null, "ADMIN", a.subjectId(), "AI_CONFIG_UPDATED", "ai", null, h.getRemoteAddr());
        return Map.of("ok", true);
    }

    @GetMapping("/models")
    public List<Map<String, Object>> models() {
        return jdbc.queryForList("SELECT id, model_id AS modelId, display_name AS displayName, active, input_cost_per_1k AS inputCostPer1k, output_cost_per_1k AS outputCostPer1k FROM ai_models ORDER BY id");
    }

    @PostMapping("/models")
    public Map<String, Boolean> addModel(@Valid @RequestBody ModelInput m) {
        try { jdbc.update("INSERT INTO ai_models(model_id,display_name,active,input_cost_per_1k,output_cost_per_1k) VALUES (?,?,?,?,?)", m.modelId(), m.displayName(), m.active(), m.inputCostPer1k(), m.outputCostPer1k()); }
        catch (org.springframework.dao.DuplicateKeyException e) { throw ApiException.conflict("MODEL_EXISTS", "Model already registered"); }
        return Map.of("ok", true);
    }

    @PutMapping("/models/{id}")
    public Map<String, Boolean> updateModel(@PathVariable long id, @Valid @RequestBody ModelInput m) {
        if (jdbc.update("UPDATE ai_models SET model_id=?,display_name=?,active=?,input_cost_per_1k=?,output_cost_per_1k=? WHERE id=?", m.modelId(), m.displayName(), m.active(), m.inputCostPer1k(), m.outputCostPer1k(), id) == 0)
            throw ApiException.notFound("Model not found");
        return Map.of("ok", true);
    }

    /** Usage per company this month with cost estimated from registered model prices (zero if a model has no price set). */
    @GetMapping("/usage")
    public List<Map<String, Object>> usage() {
        return jdbc.queryForList("""
            SELECT c.id AS companyId, c.name AS company, COUNT(u.id) AS requests, COALESCE(SUM(u.prompt_tokens),0) AS promptTokens,
                   COALESCE(SUM(u.completion_tokens),0) AS completionTokens,
                   COALESCE(SUM(u.prompt_tokens/1000*m.input_cost_per_1k + u.completion_tokens/1000*m.output_cost_per_1k),0) AS estimatedCost
            FROM ai_usage u JOIN companies c ON c.id=u.company_id LEFT JOIN ai_models m ON m.model_id=u.model
            WHERE u.created_at >= DATE_FORMAT(UTC_TIMESTAMP(),'%Y-%m-01') GROUP BY c.id, c.name ORDER BY requests DESC LIMIT 200""");
    }

    private String setting(String k) {
        List<String> r = jdbc.queryForList("SELECT setting_value FROM platform_settings WHERE setting_key=?", String.class, k);
        return r.isEmpty() ? null : r.get(0);
    }
    private void put(String k, String v) {
        jdbc.update("INSERT INTO platform_settings(setting_key,setting_value) VALUES (?,?) ON DUPLICATE KEY UPDATE setting_value=VALUES(setting_value)", k, v);
    }
}
