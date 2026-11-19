package com.fayzo.fleetpulse.workflow;

import com.fayzo.fleetpulse.common.ApiException;
import com.fayzo.fleetpulse.tenant.TenantContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Single place that changes shipment status: validates the transition and writes the timeline event (and earnings for work-based staff). */
@Service
@Transactional
public class ShipmentWorkflowService {
    public record StatusChange(String status, String note, BigDecimal lat, BigDecimal lng, String failureReason, String podUrl, String podSignedBy) {}

    private static final Map<String, Set<String>> NEXT = Map.of(
            "CREATED", Set.of("ASSIGNED", "CANCELLED"),
            "ASSIGNED", Set.of("PICKED_UP", "CANCELLED"),
            "PICKED_UP", Set.of("IN_TRANSIT", "FAILED"),
            "IN_TRANSIT", Set.of("OUT_FOR_DELIVERY", "DELIVERED", "FAILED"),
            "OUT_FOR_DELIVERY", Set.of("DELIVERED", "FAILED"),
            "FAILED", Set.of("ASSIGNED", "RETURNED"));
    public static final Set<String> DRIVER_ALLOWED = Set.of("PICKED_UP", "IN_TRANSIT", "OUT_FOR_DELIVERY", "DELIVERED", "FAILED");

    private final JdbcTemplate jdbc;
    private final com.fayzo.fleetpulse.push.PushService push;
    private final com.fayzo.fleetpulse.push.WhatsAppService whatsapp;
    public ShipmentWorkflowService(JdbcTemplate jdbc, com.fayzo.fleetpulse.push.PushService push, com.fayzo.fleetpulse.push.WhatsAppService whatsapp) { this.jdbc = jdbc; this.push = push; this.whatsapp = whatsapp; }

    /** @param onlyDriverId when non-null the shipment must be assigned to this driver (driver self-service). */
    public Map<String, Object> change(long shipmentId, StatusChange c, Long onlyDriverId) {
        long company = TenantContext.require();
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT id, status, driver_id FROM shipments WHERE id=? AND company_id=? FOR UPDATE", shipmentId, company);
        if (rows.isEmpty()) throw ApiException.notFound("Shipment not found");
        Map<String, Object> s = rows.get(0);
        Long driverId = s.get("driver_id") == null ? null : ((Number) s.get("driver_id")).longValue();
        if (onlyDriverId != null) {
            if (!onlyDriverId.equals(driverId)) throw ApiException.notFound("Shipment not found");
            if (!DRIVER_ALLOWED.contains(c.status())) throw ApiException.forbidden("Drivers cannot set status " + c.status());
        }
        String current = (String) s.get("status");
        if (!NEXT.getOrDefault(current, Set.of()).contains(c.status()))
            throw ApiException.badRequest("BAD_TRANSITION", "Cannot move a shipment from " + current + " to " + c.status());
        if ("DELIVERED".equals(c.status()) && blank(c.podUrl()) && blank(c.podSignedBy()))
            throw ApiException.badRequest("POD_REQUIRED", "Proof of delivery (photo or receiver name) is required");
        if ("FAILED".equals(c.status()) && blank(c.failureReason()))
            throw ApiException.badRequest("REASON_REQUIRED", "A failure reason is required");

        jdbc.update("""
            UPDATE shipments SET status=?, failure_reason=COALESCE(?,failure_reason), pod_url=COALESCE(?,pod_url), pod_signed_by=COALESCE(?,pod_signed_by),
              delivered_at=IF(?='DELIVERED', UTC_TIMESTAMP(3), delivered_at) WHERE id=? AND company_id=?""",
                c.status(), c.failureReason(), c.podUrl(), c.podSignedBy(), c.status(), shipmentId, company);
        jdbc.update("INSERT INTO shipment_events(company_id,shipment_id,status,note,lat,lng,occurred_at,public_visible) VALUES (?,?,?,?,?,?,UTC_TIMESTAMP(3),TRUE)",
                company, shipmentId, c.status(), c.note() != null ? c.note() : c.failureReason(), c.lat(), c.lng());

        if ("DELIVERED".equals(c.status()) && driverId != null) recordDeliveryWork(company, driverId, shipmentId);
        if ("OUT_FOR_DELIVERY".equals(c.status()) || "DELIVERED".equals(c.status())) {
            List<Map<String, Object>> info = jdbc.queryForList("SELECT tracking_id, receiver_mobile FROM shipments WHERE id=? AND company_id=?", shipmentId, company);
            if (!info.isEmpty()) whatsapp.shipmentUpdate(c.status(), (String) info.get(0).get("receiver_mobile"), (String) info.get(0).get("tracking_id"));
        }
        return Map.of("shipmentId", shipmentId, "status", c.status());
    }

    /** Assigns driver (and optionally vehicle) to a shipment, moves it to ASSIGNED and notifies the driver in-app and by push. */
    public Map<String, Object> assign(long shipmentId, long driverId, Long vehicleId) {
        long company = TenantContext.require();
        List<Map<String, Object>> d = jdbc.queryForList("SELECT user_id FROM drivers WHERE id=? AND company_id=?", driverId, company);
        if (d.isEmpty()) throw ApiException.badRequest("INVALID_REFERENCE", "Driver does not exist");
        if (vehicleId != null) {
            Integer ok = jdbc.queryForObject("SELECT COUNT(*) FROM vehicles WHERE id=? AND company_id=?", Integer.class, vehicleId, company);
            if (ok == null || ok == 0) throw ApiException.badRequest("INVALID_REFERENCE", "Vehicle does not exist");
        }
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT status, tracking_id FROM shipments WHERE id=? AND company_id=? FOR UPDATE", shipmentId, company);
        if (rows.isEmpty()) throw ApiException.notFound("Shipment not found");
        String st = (String) rows.get(0).get("status");
        if (!st.equals("CREATED") && !st.equals("FAILED") && !st.equals("ASSIGNED")) throw ApiException.badRequest("BAD_TRANSITION", "Cannot assign a shipment that is " + st);
        jdbc.update("UPDATE shipments SET driver_id=?, vehicle_id=COALESCE(?,vehicle_id), status='ASSIGNED' WHERE id=? AND company_id=?", driverId, vehicleId, shipmentId, company);
        jdbc.update("INSERT INTO shipment_events(company_id,shipment_id,status,note,occurred_at,public_visible) VALUES (?,?,'ASSIGNED','Assigned to a driver',UTC_TIMESTAMP(3),TRUE)", company, shipmentId);
        if (d.get(0).get("user_id") != null)
            push.notifyUser(company, ((Number) d.get(0).get("user_id")).longValue(), "SHIPMENT_ASSIGNED", "New shipment assigned", "Shipment " + rows.get(0).get("tracking_id") + " was assigned to you", "shipment", shipmentId);
        return Map.of("shipmentId", shipmentId, "status", "ASSIGNED");
    }

    /** A completed delivery becomes a PENDING work record for a work-based employee; a manager must approve it before it is paid. */
    private void recordDeliveryWork(long company, long driverId, long shipmentId) {
        List<Map<String, Object>> e = jdbc.queryForList("""
            SELECT e.id, ss.per_delivery_rate FROM employees e JOIN salary_structures ss ON ss.employee_id=e.id AND ss.company_id=e.company_id
            WHERE e.company_id=? AND e.driver_id=? AND ss.model='WORK_BASED'""", company, driverId);
        if (e.isEmpty() || e.get(0).get("per_delivery_rate") == null) return;
        BigDecimal rate = (BigDecimal) e.get(0).get("per_delivery_rate");
        Integer dup = jdbc.queryForObject("SELECT COUNT(*) FROM work_records WHERE company_id=? AND work_type='DELIVERY' AND reference_id=?", Integer.class, company, shipmentId);
        if (dup != null && dup > 0) return;
        jdbc.update("INSERT INTO work_records(company_id,employee_id,work_type,reference_id,quantity,rate,amount,work_date,status) VALUES (?,?,'DELIVERY',?,1,?,?,?,'PENDING')",
                company, e.get(0).get("id"), shipmentId, rate, rate, LocalDate.now());
    }

    private static boolean blank(String s) { return s == null || s.isBlank(); }
}
