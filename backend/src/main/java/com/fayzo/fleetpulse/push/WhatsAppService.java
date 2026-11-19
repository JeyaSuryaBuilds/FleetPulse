package com.fayzo.fleetpulse.push;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * WhatsApp Business Cloud API (Meta). Sends only pre-approved TEMPLATE messages and only when ALL of: token + phone number id + template name
 * are configured, and the platform feature flag whatsapp_notifications is enabled. Otherwise it silently does nothing (never reports a send that did not happen).
 * Template body is expected to take two variables: {{1}} = tracking ID, {{2}} = tracking link.
 */
@Service
public class WhatsAppService {
    private static final Logger log = LoggerFactory.getLogger(WhatsAppService.class);
    private final JdbcTemplate jdbc;
    private final String token, phoneId, tplOfd, tplDelivered, lang, publicUrl;
    private final RestClient http = RestClient.builder().baseUrl("https://graph.facebook.com/v20.0").build();

    public WhatsAppService(JdbcTemplate jdbc, @Value("${fleetpulse.whatsapp.token:}") String token, @Value("${fleetpulse.whatsapp.phone-number-id:}") String phoneId,
                           @Value("${fleetpulse.whatsapp.template-out-for-delivery:}") String tplOfd, @Value("${fleetpulse.whatsapp.template-delivered:}") String tplDelivered,
                           @Value("${fleetpulse.whatsapp.template-language:en}") String lang, @Value("${fleetpulse.public-url}") String publicUrl) {
        this.jdbc = jdbc; this.token = token; this.phoneId = phoneId; this.tplOfd = tplOfd; this.tplDelivered = tplDelivered; this.lang = lang; this.publicUrl = publicUrl;
    }

    public void shipmentUpdate(String status, String toMobile, String trackingId) {
        String template = "OUT_FOR_DELIVERY".equals(status) ? tplOfd : "DELIVERED".equals(status) ? tplDelivered : "";
        if (template.isBlank() || token.isBlank() || phoneId.isBlank() || toMobile == null || !toMobile.matches("^\\+[1-9][0-9]{7,14}$")) return;
        List<Boolean> flag = jdbc.queryForList("SELECT enabled FROM feature_flags WHERE flag_key='whatsapp_notifications'", Boolean.class);
        if (flag.isEmpty() || !flag.get(0)) return;
        String link = publicUrl + "/track/" + trackingId;
        Map<String, Object> body = Map.of("messaging_product", "whatsapp", "to", toMobile.substring(1), "type", "template",
                "template", Map.of("name", template, "language", Map.of("code", lang),
                        "components", List.of(Map.of("type", "body", "parameters", List.of(Map.of("type", "text", "text", trackingId), Map.of("type", "text", "text", link))))));
        try { http.post().uri("/" + phoneId + "/messages").header("Authorization", "Bearer " + token).body(body).retrieve().toBodilessEntity(); }
        catch (Exception e) { log.warn("WhatsApp send failed: {}", e.getClass().getSimpleName()); }
    }
}
