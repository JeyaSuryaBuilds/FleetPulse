package com.fayzo.fleetpulse.registration;

import com.fayzo.fleetpulse.audit.AuditService;
import com.fayzo.fleetpulse.auth.AuthService;
import com.fayzo.fleetpulse.common.ApiException;
import com.fayzo.fleetpulse.common.Hashing;
import com.fayzo.fleetpulse.config.AppProperties;
import com.fayzo.fleetpulse.notify.OtpDelivery;
import com.fayzo.fleetpulse.registration.RegistrationDtos.*;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class RegistrationService {

    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final OtpDelivery delivery;
    private final AuthService auth;
    private final AuditService audit;
    private final AppProperties props;

    public RegistrationService(
            JdbcTemplate jdbc,
            PasswordEncoder encoder,
            OtpDelivery delivery,
            AuthService auth,
            AuditService audit,
            AppProperties props) {

        this.jdbc = jdbc;
        this.encoder = encoder;
        this.delivery = delivery;
        this.auth = auth;
        this.audit = audit;
        this.props = props;
    }

    /**
     * Registration start.
     * OTP bypass is allowed only when dev-bypass is explicitly enabled.
     */
    @Transactional
    public StartResponse start(StartRequest r, String ip, String ua) {

        if (!r.password().equals(r.confirmPassword())) {
            throw ApiException.badRequest(
                    "PASSWORD_MISMATCH",
                    "Passwords do not match"
            );
        }

        String email = r.companyEmail().trim().toLowerCase();
        String mobile = r.mobile().trim();

        if (count(
                "SELECT COUNT(*) FROM companies WHERE email=?",
                email
        ) > 0 || count(
                "SELECT COUNT(*) FROM users WHERE email=?",
                email
        ) > 0) {

            throw ApiException.conflict(
                    "EMAIL_IN_USE",
                    "A company with this email already exists"
            );
        }

        if (count(
                "SELECT COUNT(*) FROM companies WHERE mobile=?",
                mobile
        ) > 0) {

            throw ApiException.conflict(
                    "MOBILE_IN_USE",
                    "A company with this mobile number already exists"
            );
        }

        if (r.planCode() != null &&
                count(
                        "SELECT COUNT(*) FROM plans WHERE code=? AND active=TRUE",
                        r.planCode()
                ) == 0) {

            throw ApiException.badRequest(
                    "UNKNOWN_PLAN",
                    "Selected plan is not available"
            );
        }

        jdbc.update(
                """
                DELETE FROM pending_registrations
                WHERE (company_email=? OR mobile=?)
                AND completed_at IS NULL
                """,
                email,
                mobile
        );

        String publicId = UUID.randomUUID().toString();

        String emailOtp = Hashing.otp6();
        String mobileOtp = Hashing.otp6();

        jdbc.update(
                """
                INSERT INTO pending_registrations(
                    public_id,
                    company_name,
                    company_email,
                    mobile,
                    admin_name,
                    password_hash,
                    plan_code,
                    email_otp_hash,
                    mobile_otp_hash,
                    expires_at
                )
                VALUES (?,?,?,?,?,?,?,?,?,?)
                """,
                publicId,
                r.companyName().trim(),
                email,
                mobile,
                r.adminFullName().trim(),
                encoder.encode(r.password()),
                r.planCode(),
                Hashing.sha256(publicId + ":" + emailOtp),
                Hashing.sha256(publicId + ":" + mobileOtp),
                Timestamp.from(
                        Instant.now().plus(
                                props.otp().ttlMinutes(),
                                ChronoUnit.MINUTES
                        )
                )
        );

        // LOCAL DEVELOPMENT ONLY: skip both OTP channels.
        if (props.otp().devBypass()) {

            jdbc.update(
                    """
                    UPDATE pending_registrations
                    SET email_verified=TRUE,
                        mobile_verified=TRUE
                    WHERE public_id=?
                    """,
                    publicId
            );

            Map<String, Object> p = pending(publicId);

            var session = complete(p, ip, ua);

            return new StartResponse(
                    publicId,
                    false,
                    false,
                    props.otp().ttlMinutes(),
                    true,
                    session
            );
        }

        // Normal registration flow.
        boolean emailEnabled = props.email().enabled();

        if (emailEnabled) {
            delivery.sendEmail(email, emailOtp);
        } else {
            jdbc.update(
                    """
                    UPDATE pending_registrations
                    SET email_verified=TRUE
                    WHERE public_id=?
                    """,
                    publicId
            );
        }

        delivery.sendSms(mobile, mobileOtp);

        return new StartResponse(
                publicId,
                emailEnabled,
                true,
                props.otp().ttlMinutes(),
                false,
                null
        );
    }

    @Transactional
    public VerifyResponse verify(
            String channel,
            VerifyRequest r,
            String ip,
            String ua) {

        Map<String, Object> p = pending(r.registrationId());

        boolean email = "EMAIL".equals(channel);
        String col = email ? "email" : "mobile";

        int attempts = ((Number) p.get(col + "_attempts")).intValue();

        if (attempts >= props.otp().maxAttempts()) {
            throw ApiException.tooMany(
                    "Too many incorrect codes. Restart registration."
            );
        }

        if (!asBool(p.get(col + "_verified"))) {

            String expected = (String) p.get(col + "_otp_hash");

            String actual = Hashing.sha256(
                    r.registrationId() + ":" + r.code()
            );

            if (!constantTimeEquals(expected, actual)) {

                jdbc.update(
                        "UPDATE pending_registrations SET "
                                + col + "_attempts="
                                + col + "_attempts+1 WHERE id=?",
                        p.get("id")
                );

                throw ApiException.badRequest(
                        "OTP_INVALID",
                        "The code is incorrect"
                );
            }

            jdbc.update(
                    "UPDATE pending_registrations SET "
                            + col + "_verified=TRUE WHERE id=?",
                    p.get("id")
            );

            p = pending(r.registrationId());
        }

        boolean ev = asBool(p.get("email_verified"));
        boolean mv = asBool(p.get("mobile_verified"));

        if (ev && mv) {
            return new VerifyResponse(
                    true,
                    true,
                    true,
                    complete(p, ip, ua)
            );
        }

        return new VerifyResponse(
                ev,
                mv,
                false,
                null
        );
    }

    public StartResponse resend(ResendRequest r) {

        if (props.otp().devBypass()) {
            throw ApiException.badRequest(
                    "OTP_DISABLED",
                    "OTP resend is disabled in local testing mode."
            );
        }

        Map<String, Object> p = pending(r.registrationId());

        Timestamp last = (Timestamp) p.get("last_sent_at");

        if (last != null &&
                last.toInstant()
                        .plusSeconds(props.otp().resendCooldownSeconds())
                        .isAfter(Instant.now())) {

            throw ApiException.tooMany(
                    "Please wait before requesting another code"
            );
        }

        String code = Hashing.otp6();

        boolean email = "EMAIL".equals(r.channel());
        String col = email ? "email" : "mobile";

        String updateSql =
                "UPDATE pending_registrations SET "
                        + col + "_otp_hash=?, "
                        + col + "_attempts=0, "
                        + "last_sent_at=UTC_TIMESTAMP(), "
                        + "expires_at=? "
                        + "WHERE id=?";

        jdbc.update(
                updateSql,
                Hashing.sha256(r.registrationId() + ":" + code),
                Timestamp.from(
                        Instant.now().plus(
                                props.otp().ttlMinutes(),
                                ChronoUnit.MINUTES
                        )
                ),
                p.get("id")
        );

        if (email) {
            if (!props.email().enabled()) {
                throw ApiException.badRequest(
                        "EMAIL_DISABLED",
                        "Email delivery is disabled."
                );
            }

            delivery.sendEmail(
                    (String) p.get("company_email"),
                    code
            );

        } else {
            delivery.sendSms(
                    (String) p.get("mobile"),
                    code
            );
        }

        return new StartResponse(
                r.registrationId(),
                email,
                !email,
                props.otp().ttlMinutes(),
                false,
                null
        );
    }

    private com.fayzo.fleetpulse.auth.AuthDtos.AuthResponse complete(
            Map<String, Object> p,
            String ip,
            String ua) {

        String email = (String) p.get("company_email");
        String mobile = (String) p.get("mobile");

        if (count(
                "SELECT COUNT(*) FROM companies WHERE email=? OR mobile=?",
                email,
                mobile
        ) > 0) {

            throw ApiException.conflict(
                    "ALREADY_REGISTERED",
                    "This company is already registered"
            );
        }

        long companyId = insertReturningId(
                """
                INSERT INTO companies(
                    name,
                    email,
                    mobile,
                    status,
                    email_verified_at,
                    mobile_verified_at
                )
                VALUES (?,?,?, 'TRIAL', UTC_TIMESTAMP(), UTC_TIMESTAMP())
                """,
                p.get("company_name"),
                email,
                mobile
        );

        long userId = insertReturningId(
                """
                INSERT INTO users(
                    company_id,
                    full_name,
                    email,
                    mobile,
                    password_hash,
                    role_code
                )
                VALUES (?,?,?,?,?, 'COMPANY_ADMIN')
                """,
                companyId,
                p.get("admin_name"),
                email,
                mobile,
                p.get("password_hash")
        );

        String planCode = p.get("plan_code") == null
                ? "STARTER"
                : (String) p.get("plan_code");

        Map<String, Object> plan = jdbc.queryForMap(
                "SELECT id, trial_days FROM plans WHERE code=?",
                planCode
        );

        int trial = ((Number) plan.get("trial_days")).intValue();

        if (trial <= 0) {
            trial = props.trialDays();
        }

        jdbc.update(
                """
                INSERT INTO subscriptions(
                    company_id,
                    plan_id,
                    status,
                    billing_cycle,
                    trial_ends_at,
                    current_period_start,
                    current_period_end
                )
                VALUES (
                    ?,
                    ?,
                    'TRIALING',
                    'MONTHLY',
                    DATE_ADD(UTC_TIMESTAMP(), INTERVAL ? DAY),
                    UTC_TIMESTAMP(),
                    DATE_ADD(UTC_TIMESTAMP(), INTERVAL ? DAY)
                )
                """,
                companyId,
                plan.get("id"),
                trial,
                trial
        );

        jdbc.update(
                """
                UPDATE pending_registrations
                SET completed_at=UTC_TIMESTAMP()
                WHERE id=?
                """,
                p.get("id")
        );

        audit.log(
                companyId,
                "USER",
                userId,
                "COMPANY_REGISTERED",
                "company",
                String.valueOf(companyId),
                ip
        );

        return auth.buildResponse(userId, ip, ua);
    }

    private Map<String, Object> pending(String publicId) {

        List<Map<String, Object>> rows = jdbc.queryForList(
                """
                SELECT *
                FROM pending_registrations
                WHERE public_id=?
                AND completed_at IS NULL
                AND expires_at > UTC_TIMESTAMP()
                """,
                publicId
        );

        if (rows.isEmpty()) {
            throw ApiException.badRequest(
                    "REGISTRATION_EXPIRED",
                    "This registration has expired. Please start again."
            );
        }

        return rows.get(0);
    }

    private long insertReturningId(String sql, Object... args) {

        KeyHolder kh = new GeneratedKeyHolder();

        jdbc.update(con -> {

            PreparedStatement ps = con.prepareStatement(
                    sql,
                    Statement.RETURN_GENERATED_KEYS
            );

            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }

            return ps;

        }, kh);

        return kh.getKey().longValue();
    }

    private int count(String sql, Object... args) {

        Integer n = jdbc.queryForObject(
                sql,
                Integer.class,
                args
        );

        return n == null ? 0 : n;
    }

    private static boolean asBool(Object o) {

        return o instanceof Boolean b
                ? b
                : o instanceof Number n && n.intValue() != 0;
    }

    private static boolean constantTimeEquals(String a, String b) {

        return java.security.MessageDigest.isEqual(
                a.getBytes(),
                b.getBytes()
        );
    }
}