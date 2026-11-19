package com.fayzo.fleetpulse.audit;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private final JdbcTemplate jdbc;
    public AuditService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public void log(Long companyId, String actorType, Long actorId, String action, String entityType, String entityId, String ip) {
        jdbc.update("INSERT INTO audit_logs(company_id,actor_type,actor_id,action,entity_type,entity_id,ip_address) VALUES (?,?,?,?,?,?,?)",
                companyId, actorType, actorId, action, entityType, entityId, ip);
    }

    public void loginActivity(String subjectType, Long subjectId, String identifier, boolean success, String reason, String ip, String ua) {
        jdbc.update("INSERT INTO login_activity(subject_type,subject_id,identifier,success,reason,ip_address,user_agent) VALUES (?,?,?,?,?,?,?)",
                subjectType, subjectId, identifier, success, reason, ip, ua == null ? null : ua.substring(0, Math.min(300, ua.length())));
    }
}
