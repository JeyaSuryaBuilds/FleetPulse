package com.fayzo.fleetpulse.tracking;

import com.fayzo.fleetpulse.common.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Unauthenticated customer tracking. Exposes only customer-facing fields: no prices, customer records, driver or
 * vehicle identity, or other shipments. Location is returned only when a recent REAL GPS report exists.
 * Rate limited per IP (see RateLimitFilter). Unknown IDs return a uniform 404.
 */
@RestController
@RequestMapping("/api/v1/public/tracking")
public class PublicTrackingController {
    private final JdbcTemplate jdbc;
    public PublicTrackingController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @GetMapping("/{trackingId}")
    public Map<String, Object> track(@PathVariable String trackingId) {
        if (!trackingId.matches("^[A-Za-z0-9-]{6,30}$")) throw ApiException.notFound("Shipment not found");
        List<Map<String, Object>> rows = jdbc.queryForList("""
            SELECT s.id, s.company_id, s.tracking_id, s.status, s.pickup_address, s.destination_address, s.package_count, s.scheduled_delivery,
                   s.delivered_at, s.vehicle_id, c.name AS company_name
            FROM shipments s JOIN companies c ON c.id=s.company_id WHERE s.tracking_id=?""", trackingId.toUpperCase());
        if (rows.isEmpty()) throw ApiException.notFound("Shipment not found");
        Map<String, Object> s = rows.get(0);
        long shipmentId = ((Number) s.get("id")).longValue(), companyId = ((Number) s.get("company_id")).longValue();

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("trackingId", s.get("tracking_id"));
        out.put("company", s.get("company_name"));
        out.put("status", s.get("status"));
        out.put("pickup", s.get("pickup_address"));
        out.put("destination", s.get("destination_address"));
        out.put("packageCount", s.get("package_count"));
        out.put("estimatedDelivery", s.get("scheduled_delivery"));
        out.put("deliveredAt", s.get("delivered_at"));
        out.put("timeline", jdbc.queryForList(
                "SELECT status, note, occurred_at AS occurredAt FROM shipment_events WHERE shipment_id=? AND company_id=? AND public_visible=TRUE ORDER BY occurred_at", shipmentId, companyId));

        Map<String, Object> loc = null;
        String status = (String) s.get("status");
        if (s.get("vehicle_id") != null && ("IN_TRANSIT".equals(status) || "OUT_FOR_DELIVERY".equals(status))) {
            List<Map<String, Object>> g = jdbc.queryForList("""
                SELECT latitude, longitude, recorded_at AS recordedAt FROM gps_locations
                WHERE company_id=? AND vehicle_id=? AND recorded_at > DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 15 MINUTE)
                ORDER BY recorded_at DESC LIMIT 1""", companyId, s.get("vehicle_id"));
            if (!g.isEmpty()) loc = g.get(0);
        }
        out.put("currentLocation", loc);
        out.put("locationAvailable", loc != null);
        return out;
    }
}
