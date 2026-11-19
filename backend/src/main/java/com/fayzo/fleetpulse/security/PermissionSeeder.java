package com.fayzo.fleetpulse.security;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Keeps the permissions / role_permissions tables in step with the Role and Permission enums (the enums remain the enforced source). */
@Component
@Order(1)
public class PermissionSeeder implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    public PermissionSeeder(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public void run(ApplicationArguments args) {
        for (Permission p : Permission.values()) {
            String module = p.name().contains("_") ? p.name().substring(0, p.name().indexOf('_')).toLowerCase() : p.name().toLowerCase();
            jdbc.update("INSERT IGNORE INTO permissions(code,module) VALUES (?,?)", p.name(), module);
        }
        for (Role r : Role.values()) {
            for (Permission p : r.permissions()) {
                jdbc.update("INSERT IGNORE INTO role_permissions(role_id,permission_id) SELECT r.id, p.id FROM roles r, permissions p WHERE r.code=? AND p.code=?", r.name(), p.name());
            }
        }
    }
}
