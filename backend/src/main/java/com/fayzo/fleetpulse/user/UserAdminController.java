package com.fayzo.fleetpulse.user;

import com.fayzo.fleetpulse.audit.AuditService;
import com.fayzo.fleetpulse.common.ApiException;
import com.fayzo.fleetpulse.common.Authz;
import com.fayzo.fleetpulse.security.AuthPrincipal;
import com.fayzo.fleetpulse.security.Permission;
import com.fayzo.fleetpulse.security.Role;
import com.fayzo.fleetpulse.subscription.PlanLimits;
import com.fayzo.fleetpulse.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** User and role management for the caller's own company only. Every query is bound to the tenant. */
@RestController
@RequestMapping("/api/v1/users")
@Transactional
public class UserAdminController {
    public record CreateUser(@NotBlank @Size(max = 160) String fullName, @NotBlank @Email String email, @Size(max = 24) String mobile,
                             @NotBlank @Size(min = 10, max = 128) String password, @NotBlank String role) {}
    public record UpdateUser(@Size(max = 160) String fullName, String role, @Pattern(regexp = "ACTIVE|DISABLED") String status,
                             @Size(min = 10, max = 128) String newPassword) {}

    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final PlanLimits limits;
    private final AuditService audit;

    public UserAdminController(JdbcTemplate jdbc, PasswordEncoder encoder, PlanLimits limits, AuditService audit) {
        this.jdbc = jdbc; this.encoder = encoder; this.limits = limits; this.audit = audit;
    }

    @GetMapping
    public List<Map<String, Object>> list() {
        Authz.require(Permission.USER_MANAGE);
        return jdbc.queryForList("SELECT id, full_name AS fullName, email, mobile, role_code AS role, status, last_login_at AS lastLoginAt FROM users WHERE company_id=? ORDER BY id", TenantContext.require());
    }

    @GetMapping("/roles")
    public List<String> roles() {
        Authz.require(Permission.USER_MANAGE);
        return java.util.Arrays.stream(Role.values()).map(Enum::name).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(@Valid @RequestBody CreateUser r, HttpServletRequest http) {
        Authz.require(Permission.USER_MANAGE);
        long c = TenantContext.require();
        Role role = parseRole(r.role());
        limits.requireUserCapacity();
        String email = r.email().trim().toLowerCase();
        Integer exists = jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE email=?", Integer.class, email);
        if (exists != null && exists > 0) throw ApiException.conflict("EMAIL_IN_USE", "A user with this email already exists");
        jdbc.update("INSERT INTO users(company_id,full_name,email,mobile,password_hash,role_code) VALUES (?,?,?,?,?,?)",
                c, r.fullName().trim(), email, r.mobile(), encoder.encode(r.password()), role.name());
        Long id = jdbc.queryForObject("SELECT id FROM users WHERE email=?", Long.class, email);
        AuthPrincipal p = Authz.principal();
        audit.log(c, p.type(), p.subjectId(), "CREATE_USER", "user", String.valueOf(id), http.getRemoteAddr());
        return Map.of("id", id, "email", email, "role", role.name());
    }

    @PatchMapping("/{id}")
    public Map<String, Boolean> update(@PathVariable long id, @Valid @RequestBody UpdateUser u, HttpServletRequest http) {
        Authz.require(Permission.USER_MANAGE);
        long c = TenantContext.require();
        AuthPrincipal me = Authz.principal();
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT role_code FROM users WHERE id=? AND company_id=?", id, c);
        if (rows.isEmpty()) throw ApiException.notFound("User not found");
        boolean self = me.subjectId() == id;
        if (self && (u.status() != null || u.role() != null)) throw ApiException.badRequest("SELF_CHANGE", "You cannot change your own role or status");
        if (u.role() != null) jdbc.update("UPDATE users SET role_code=?, token_version=token_version+1 WHERE id=? AND company_id=?", parseRole(u.role()).name(), id, c);
        if (u.status() != null) jdbc.update("UPDATE users SET status=?, token_version=token_version+1 WHERE id=? AND company_id=?", u.status(), id, c);
        if (u.fullName() != null) jdbc.update("UPDATE users SET full_name=? WHERE id=? AND company_id=?", u.fullName().trim(), id, c);
        if (u.newPassword() != null) jdbc.update("UPDATE users SET password_hash=?, token_version=token_version+1, failed_attempts=0, locked_until=NULL WHERE id=? AND company_id=?", encoder.encode(u.newPassword()), id, c);
        guardLastAdmin(c);
        audit.log(c, me.type(), me.subjectId(), "UPDATE_USER", "user", String.valueOf(id), http.getRemoteAddr());
        return Map.of("ok", true);
    }

    private void guardLastAdmin(long companyId) {
        Integer admins = jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE company_id=? AND role_code='COMPANY_ADMIN' AND status='ACTIVE'", Integer.class, companyId);
        if (admins == null || admins == 0) throw ApiException.badRequest("LAST_ADMIN", "The company must keep at least one active Company Admin");
    }

    private Role parseRole(String s) {
        try { return Role.valueOf(s); } catch (Exception e) { throw ApiException.badRequest("BAD_ROLE", "Unknown role"); }
    }
}
