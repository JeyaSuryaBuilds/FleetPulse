package com.fayzo.fleetpulse.push;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.*;

/**
 * Stores an in-app notification and sends a push through the Expo Push service to the user's registered devices (Android and iOS apps
 * use Expo). Push failures never break the calling business operation. FCM/APNs credentials are configured in Expo (EAS), not here.
 */
@Service
public class PushService {
    private static final Logger log = LoggerFactory.getLogger(PushService.class);
    private final JdbcTemplate jdbc;
    private final RestClient http = RestClient.builder().baseUrl("https://exp.host").build();
    public PushService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public void notifyUser(long companyId, long userId, String type, String title, String message, String entityType, Long entityId) {
        jdbc.update("INSERT INTO notifications(company_id,user_id,type,severity,title,message,entity_type,entity_id) VALUES (?,?,?,'INFO',?,?,?,?)",
                companyId, userId, type, title, message, entityType, entityId);
        List<String> tokens = jdbc.queryForList("SELECT token FROM device_tokens WHERE company_id=? AND user_id=?", String.class, companyId, userId);
        if (tokens.isEmpty()) return;
        List<Map<String, Object>> msgs = new ArrayList<>();
        for (String t : tokens) msgs.add(Map.of("to", t, "title", title, "body", message == null ? "" : message, "sound", "default",
                "data", Map.of("type", type, "entityType", entityType == null ? "" : entityType, "entityId", entityId == null ? 0 : entityId)));
        try { http.post().uri("/--/api/v2/push/send").body(msgs).retrieve().toBodilessEntity(); }
        catch (Exception e) { log.warn("Push delivery failed: {}", e.getClass().getSimpleName()); }
    }
}
