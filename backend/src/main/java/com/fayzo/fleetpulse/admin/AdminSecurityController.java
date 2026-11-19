package com.fayzo.fleetpulse.admin;

import com.fayzo.fleetpulse.audit.AuditService;
import com.fayzo.fleetpulse.common.ApiException;
import com.fayzo.fleetpulse.security.AuthPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/security")
public class AdminSecurityController {
    public record ChangePassword(@NotBlank String currentPassword, @NotBlank @Size(min = 12, max = 128, message = "New password must be at least 12 characters") String newPassword) {}

    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final AuditService audit;
    public AdminSecurityController(JdbcTemplate jdbc, PasswordEncoder encoder, AuditService audit) { this.jdbc = jdbc; this.encoder = encoder; this.audit = audit; }

    /** Rotates the single Super Admin password and invalidates all admin sessions. Strongly recommended right after first login. */
    @PostMapping("/change-password")
    public Map<String, Boolean> changePassword(@Valid @RequestBody ChangePassword r, @AuthenticationPrincipal AuthPrincipal admin, HttpServletRequest http) {
        String hash = jdbc.queryForObject("SELECT password_hash FROM platform_admins WHERE id=?", String.class, admin.subjectId());
        if (hash == null || !encoder.matches(r.currentPassword(), hash)) throw ApiException.badRequest("BAD_PASSWORD", "Current password is incorrect");
        jdbc.update("UPDATE platform_admins SET password_hash=?, token_version=token_version+1 WHERE id=?", encoder.encode(r.newPassword()), admin.subjectId());
        audit.log(null, "ADMIN", admin.subjectId(), "ADMIN_PASSWORD_CHANGED", null, null, http.getRemoteAddr());
        return Map.of("ok", true);
    }

    @GetMapping("/login-activity")
    public List<Map<String, Object>> loginActivity(@RequestParam(defaultValue = "ADMIN") String subjectType, @RequestParam(required = false) Boolean success) {
        if (success == null)
            return jdbc.queryForList("SELECT id, subject_type AS subjectType, identifier, success, reason, ip_address AS ipAddress, created_at AS createdAt FROM login_activity WHERE subject_type=? ORDER BY id DESC LIMIT 300", subjectType);
        return jdbc.queryForList("SELECT id, subject_type AS subjectType, identifier, success, reason, ip_address AS ipAddress, created_at AS createdAt FROM login_activity WHERE subject_type=? AND success=? ORDER BY id DESC LIMIT 300", subjectType, success);
    }

    @GetMapping("/audit-logs")
    public List<Map<String, Object>> auditLogs(@RequestParam(required = false) Long companyId) {
        if (companyId == null)
            return jdbc.queryForList("SELECT id, company_id AS companyId, actor_type AS actorType, actor_id AS actorId, action, entity_type AS entityType, entity_id AS entityId, ip_address AS ipAddress, created_at AS createdAt FROM audit_logs ORDER BY id DESC LIMIT 300");
        return jdbc.queryForList("SELECT id, company_id AS companyId, actor_type AS actorType, actor_id AS actorId, action, entity_type AS entityType, entity_id AS entityId, ip_address AS ipAddress, created_at AS createdAt FROM audit_logs WHERE company_id=? ORDER BY id DESC LIMIT 300", companyId);
    }
}
