package com.fayzo.fleetpulse.push;

import com.fayzo.fleetpulse.common.Authz;
import com.fayzo.fleetpulse.tenant.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/me")
public class DeviceController {
    public record Device(@NotBlank @Size(max = 300) @Pattern(regexp = "^(Expo|Exponent)PushToken\\[[A-Za-z0-9_-]+\\]$", message = "Not an Expo push token") String token,
                         @NotBlank @Pattern(regexp = "ANDROID|IOS") String platform) {}
    private final JdbcTemplate jdbc;
    public DeviceController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @PostMapping("/devices")
    public Map<String, Boolean> register(@Valid @RequestBody Device d) {
        long uid = Authz.principal().subjectId(), c = TenantContext.require();
        jdbc.update("INSERT INTO device_tokens(company_id,user_id,token,platform) VALUES (?,?,?,?) ON DUPLICATE KEY UPDATE company_id=VALUES(company_id), user_id=VALUES(user_id), platform=VALUES(platform)", c, uid, d.token(), d.platform());
        return Map.of("ok", true);
    }

    @DeleteMapping("/devices")
    public Map<String, Boolean> unregister(@RequestParam String token) {
        jdbc.update("DELETE FROM device_tokens WHERE token=? AND user_id=? AND company_id=?", token, Authz.principal().subjectId(), TenantContext.require());
        return Map.of("ok", true);
    }

    @GetMapping("/notifications")
    public List<Map<String, Object>> myNotifications() {
        Authz.require(com.fayzo.fleetpulse.security.Permission.NOTIFICATION_READ);
        return jdbc.queryForList("SELECT id, type, severity, title, message, entity_type AS entityType, entity_id AS entityId, read_at AS readAt, created_at AS createdAt FROM notifications WHERE company_id=? AND (user_id IS NULL OR user_id=?) ORDER BY id DESC LIMIT 100",
                TenantContext.require(), Authz.principal().subjectId());
    }

    @PostMapping("/notifications/{id}/read")
    public Map<String, Boolean> markRead(@PathVariable long id) {
        jdbc.update("UPDATE notifications SET read_at=UTC_TIMESTAMP(3) WHERE id=? AND company_id=? AND user_id=? AND read_at IS NULL", id, TenantContext.require(), Authz.principal().subjectId());
        return Map.of("ok", true);
    }
}
