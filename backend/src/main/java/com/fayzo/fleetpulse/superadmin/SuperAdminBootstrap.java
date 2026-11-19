package com.fayzo.fleetpulse.superadmin;

import com.fayzo.fleetpulse.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the single platform admin on first start from FP_SUPERADMIN_BOOTSTRAP_PASSWORD, storing only the
 * BCrypt hash. After the first start remove the variable from the environment. Nothing is hardcoded in source.
 */
@Component
public class SuperAdminBootstrap implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(SuperAdminBootstrap.class);
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final AppProperties props;

    public SuperAdminBootstrap(JdbcTemplate jdbc, PasswordEncoder encoder, AppProperties props) {
        this.jdbc = jdbc; this.encoder = encoder; this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM platform_admins", Integer.class);
        if (n != null && n > 0) return;
        String pw = props.superadmin().bootstrapPassword();
        if (pw == null || pw.isBlank()) {
            log.warn("No platform admin exists and FP_SUPERADMIN_BOOTSTRAP_PASSWORD is not set: /admin login is unavailable.");
            return;
        }
        jdbc.update("INSERT INTO platform_admins(password_hash) VALUES (?)", encoder.encode(pw));
        log.info("Platform admin created from bootstrap password (hash stored). Remove FP_SUPERADMIN_BOOTSTRAP_PASSWORD now.");
    }
}
