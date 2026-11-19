package com.fayzo.fleetpulse.dashboard;

import com.fayzo.fleetpulse.common.Authz;
import com.fayzo.fleetpulse.security.Permission;
import com.fayzo.fleetpulse.tenant.TenantContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/** Company dashboard KPIs computed from real rows. Every query is bound to the caller's company_id. */
@RestController
@RequestMapping("/api/v1/dashboard")
@Transactional(readOnly = true)
public class DashboardController {
    private final JdbcTemplate jdbc;
    public DashboardController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    private long n(String sql, Object... a) { Long v = jdbc.queryForObject(sql, Long.class, a); return v == null ? 0 : v; }
    private Object sum(String sql, Object... a) { Object v = jdbc.queryForObject(sql, java.math.BigDecimal.class, a); return v == null ? java.math.BigDecimal.ZERO : v; }

    @GetMapping
    public Map<String, Object> stats() {
        Authz.require(Permission.DASHBOARD_VIEW);
        long c = TenantContext.require();
        String month = "DATE_FORMAT(UTC_TIMESTAMP(),'%Y-%m-01')";
        Map<String, Object> o = new LinkedHashMap<>();
        o.put("totalVehicles", n("SELECT COUNT(*) FROM vehicles WHERE company_id=? AND status NOT IN ('SOLD','RETIRED')", c));
        o.put("activeVehicles", n("SELECT COUNT(*) FROM vehicles WHERE company_id=? AND status='IN_USE'", c));
        o.put("availableVehicles", n("SELECT COUNT(*) FROM vehicles WHERE company_id=? AND status='AVAILABLE'", c));
        o.put("maintenanceVehicles", n("SELECT COUNT(*) FROM vehicles WHERE company_id=? AND status='MAINTENANCE'", c));
        o.put("totalDrivers", n("SELECT COUNT(*) FROM drivers WHERE company_id=?", c));
        o.put("totalEmployees", n("SELECT COUNT(*) FROM employees WHERE company_id=? AND status='ACTIVE'", c));
        o.put("activeShipments", n("SELECT COUNT(*) FROM shipments WHERE company_id=? AND status IN ('PICKED_UP','IN_TRANSIT','OUT_FOR_DELIVERY')", c));
        o.put("pendingShipments", n("SELECT COUNT(*) FROM shipments WHERE company_id=? AND status IN ('CREATED','ASSIGNED')", c));
        o.put("deliveredShipments", n("SELECT COUNT(*) FROM shipments WHERE company_id=? AND status='DELIVERED'", c));
        o.put("delayedShipments", n("SELECT COUNT(*) FROM shipments WHERE company_id=? AND scheduled_delivery < UTC_TIMESTAMP() AND status NOT IN ('DELIVERED','CANCELLED','FAILED','RETURNED')", c));
        o.put("revenueMonthToDate", sum("SELECT SUM(amount) FROM payments WHERE company_id=? AND direction='IN' AND paid_at >= " + month, c));
        o.put("expensesMonthToDate", sum("SELECT SUM(amount) FROM expenses WHERE company_id=? AND expense_date >= " + month, c));
        o.put("fuelExpensesMonthToDate", sum("SELECT SUM(total_cost) FROM fuel_records WHERE company_id=? AND fuel_date >= " + month, c));
        o.put("maintenanceExpensesMonthToDate", sum("SELECT SUM(cost) FROM maintenance_records WHERE company_id=? AND COALESCE(completed_on, scheduled_on) >= " + month, c));
        o.put("shipmentsByStatus", jdbc.queryForList("SELECT status, COUNT(*) AS count FROM shipments WHERE company_id=? GROUP BY status", c));
        o.put("shipmentsPerDay", jdbc.queryForList("SELECT DATE(created_at) AS day, COUNT(*) AS count FROM shipments WHERE company_id=? AND created_at >= DATE_SUB(UTC_TIMESTAMP(), INTERVAL 30 DAY) GROUP BY DATE(created_at) ORDER BY day", c));
        o.put("recentActivity", jdbc.queryForList("SELECT action, entity_type AS entityType, entity_id AS entityId, created_at AS createdAt FROM audit_logs WHERE company_id=? ORDER BY id DESC LIMIT 15", c));
        o.put("expiringDocuments", jdbc.queryForList("SELECT vehicle_id AS vehicleId, doc_type AS docType, expiry_date AS expiryDate FROM vehicle_documents WHERE company_id=? AND expiry_date IS NOT NULL AND expiry_date <= DATE_ADD(CURDATE(), INTERVAL 30 DAY) ORDER BY expiry_date LIMIT 20", c));
        o.put("expiringLicences", jdbc.queryForList("SELECT id AS driverId, full_name AS fullName, licence_expiry AS licenceExpiry FROM drivers WHERE company_id=? AND licence_expiry IS NOT NULL AND licence_expiry <= DATE_ADD(CURDATE(), INTERVAL 30 DAY) ORDER BY licence_expiry LIMIT 20", c));
        return o;
    }
}
