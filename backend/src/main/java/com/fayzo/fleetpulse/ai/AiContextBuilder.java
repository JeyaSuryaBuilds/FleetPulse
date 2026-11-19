package com.fayzo.fleetpulse.ai;

import com.fayzo.fleetpulse.common.Authz;
import com.fayzo.fleetpulse.security.Permission;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Builds the ONLY data the model ever sees: fixed aggregate queries bound to the caller's company and filtered by the caller's
 * permissions (e.g. finance figures require FINANCE_READ, payroll is never included). The model has no tools and no database access,
 * so it cannot read other tenants or perform actions.
 */
@Component
public class AiContextBuilder {
    private final JdbcTemplate jdbc;
    public AiContextBuilder(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public enum Feature {
        ASSISTANT("Answer questions about the company's fleet operations using only the data provided.", Set.of("fleet", "shipments", "fuel", "maintenance", "drivers", "finance")),
        FUEL_ANALYSIS("Analyse fuel cost and consumption. Point out vehicles with unusually high cost or poor efficiency and suggest concrete actions.", Set.of("fuel", "fleet")),
        DELAY_PREDICTION("Assess the risk of delivery delays from the historical on-time statistics and the currently open shipments. This is a statistical judgement from the data given, not a trained forecasting model; say so and state your confidence.", Set.of("shipments", "drivers")),
        ROUTE_RECOMMENDATION("Recommend improvements to routing and dispatching from the journey statistics provided. Do not invent road or traffic data.", Set.of("journeys", "fleet")),
        MAINTENANCE_INSIGHTS("Highlight maintenance risks, overdue services and cost drivers, and suggest a prioritised plan.", Set.of("maintenance", "fleet")),
        DRIVER_PERFORMANCE("Summarise driver performance fairly from the figures provided; avoid judging individuals on very small samples.", Set.of("drivers")),
        EXPENSE_ANALYSIS("Analyse expenses and margins, highlight trends and savings opportunities.", Set.of("finance", "fuel", "maintenance")),
        REPORT("Write a clear natural-language operational report with short sections and bullet points.", Set.of("fleet", "shipments", "fuel", "maintenance", "drivers", "finance"));

        public final String instruction;
        public final Set<String> sections;
        Feature(String i, Set<String> s) { instruction = i; sections = s; }
    }

    public Map<String, Object> build(Feature f, long c) {
        Map<String, Object> ctx = new LinkedHashMap<>();
        for (String s : f.sections) {
            switch (s) {
                case "fleet" -> { if (Authz.has(Permission.FLEET_READ) || Authz.has(Permission.DASHBOARD_VIEW)) ctx.put("fleet", fleet(c)); }
                case "shipments" -> { if (Authz.has(Permission.SHIPMENT_READ)) ctx.put("shipments", shipments(c)); }
                case "fuel" -> { if (Authz.has(Permission.FUEL_READ)) ctx.put("fuel_last_90_days", fuel(c)); }
                case "maintenance" -> { if (Authz.has(Permission.MAINTENANCE_READ)) ctx.put("maintenance", maintenance(c)); }
                case "drivers" -> { if (Authz.has(Permission.DRIVER_READ)) ctx.put("drivers_last_90_days", drivers(c)); }
                case "journeys" -> { if (Authz.has(Permission.JOURNEY_READ)) ctx.put("journeys_last_90_days", journeys(c)); }
                case "finance" -> { if (Authz.has(Permission.FINANCE_READ)) ctx.put("finance_last_90_days", finance(c)); }
                default -> { }
            }
        }
        return ctx;
    }

    private Object fleet(long c) {
        return Map.of(
                "vehiclesByStatus", jdbc.queryForList("SELECT status, COUNT(*) AS count FROM vehicles WHERE company_id=? GROUP BY status", c),
                "vehicles", jdbc.queryForList("SELECT registration_number AS reg, make, model, status, fuel_type AS fuelType, odometer_km AS odometerKm FROM vehicles WHERE company_id=? ORDER BY id LIMIT 60", c));
    }
    private Object shipments(long c) {
        return Map.of(
                "byStatus", jdbc.queryForList("SELECT status, COUNT(*) AS count FROM shipments WHERE company_id=? AND created_at >= DATE_SUB(UTC_TIMESTAMP(), INTERVAL 90 DAY) GROUP BY status", c),
                "onTimeStats", jdbc.queryForList("""
                    SELECT COUNT(*) AS delivered, SUM(delivered_at <= scheduled_delivery) AS onTime, ROUND(AVG(TIMESTAMPDIFF(MINUTE, scheduled_delivery, delivered_at))) AS avgMinutesVsSchedule
                    FROM shipments WHERE company_id=? AND status='DELIVERED' AND scheduled_delivery IS NOT NULL AND delivered_at >= DATE_SUB(UTC_TIMESTAMP(), INTERVAL 90 DAY)""", c),
                "openShipments", jdbc.queryForList("""
                    SELECT tracking_id AS tracking, status, scheduled_delivery AS scheduledDelivery, (scheduled_delivery < UTC_TIMESTAMP()) AS overdue
                    FROM shipments WHERE company_id=? AND status NOT IN ('DELIVERED','CANCELLED','FAILED','RETURNED') ORDER BY scheduled_delivery LIMIT 40""", c));
    }
    private Object fuel(long c) {
        return jdbc.queryForList("""
            SELECT v.registration_number AS reg, ROUND(SUM(f.litres),1) AS litres, ROUND(SUM(f.total_cost),2) AS cost, COUNT(*) AS fillUps,
                   ROUND(MAX(f.odometer_km)-MIN(f.odometer_km),0) AS odometerSpanKm
            FROM fuel_records f JOIN vehicles v ON v.id=f.vehicle_id AND v.company_id=f.company_id
            WHERE f.company_id=? AND f.fuel_date >= DATE_SUB(UTC_TIMESTAMP(), INTERVAL 90 DAY) GROUP BY v.registration_number ORDER BY cost DESC LIMIT 40""", c);
    }
    private Object maintenance(long c) {
        return Map.of(
                "open", jdbc.queryForList("""
                    SELECT v.registration_number AS reg, m.kind, m.title, m.status, m.scheduled_on AS scheduledOn, m.next_due_date AS nextDueDate FROM maintenance_records m
                    JOIN vehicles v ON v.id=m.vehicle_id AND v.company_id=m.company_id WHERE m.company_id=? AND m.status NOT IN ('COMPLETED','CLOSED','CANCELLED') ORDER BY m.scheduled_on LIMIT 40""", c),
                "costByKind90d", jdbc.queryForList("SELECT kind, ROUND(SUM(cost),2) AS cost, COUNT(*) AS count FROM maintenance_records WHERE company_id=? AND COALESCE(completed_on,scheduled_on) >= DATE_SUB(CURDATE(), INTERVAL 90 DAY) GROUP BY kind", c));
    }
    private Object drivers(long c) {
        // names only: no contact details, licence numbers or pay
        return jdbc.queryForList("""
            SELECT d.full_name AS driver,
              (SELECT COUNT(*) FROM shipments s WHERE s.driver_id=d.id AND s.company_id=d.company_id AND s.status='DELIVERED' AND s.delivered_at >= DATE_SUB(UTC_TIMESTAMP(), INTERVAL 90 DAY)) AS delivered,
              (SELECT COUNT(*) FROM shipments s WHERE s.driver_id=d.id AND s.company_id=d.company_id AND s.status='DELIVERED' AND s.delivered_at <= s.scheduled_delivery AND s.delivered_at >= DATE_SUB(UTC_TIMESTAMP(), INTERVAL 90 DAY)) AS onTime,
              (SELECT COUNT(*) FROM shipments s WHERE s.driver_id=d.id AND s.company_id=d.company_id AND s.status='FAILED' AND s.created_at >= DATE_SUB(UTC_TIMESTAMP(), INTERVAL 90 DAY)) AS failed,
              (SELECT COUNT(*) FROM driver_accidents a WHERE a.driver_id=d.id AND a.company_id=d.company_id AND a.occurred_on >= DATE_SUB(CURDATE(), INTERVAL 365 DAY)) AS accidentsLastYear
            FROM drivers d WHERE d.company_id=? ORDER BY d.id LIMIT 60""", c);
    }
    private Object journeys(long c) {
        return jdbc.queryForList("""
            SELECT status, COUNT(*) AS count, ROUND(AVG(planned_distance_km),1) AS avgPlannedKm, ROUND(AVG(actual_distance_km),1) AS avgActualKm,
                   ROUND(AVG(TIMESTAMPDIFF(MINUTE, planned_start, actual_start))) AS avgStartDelayMin, ROUND(AVG(TIMESTAMPDIFF(MINUTE, planned_end, actual_end))) AS avgEndDelayMin
            FROM journeys WHERE company_id=? AND created_at >= DATE_SUB(UTC_TIMESTAMP(), INTERVAL 90 DAY) GROUP BY status""", c);
    }
    private Object finance(long c) {
        return Map.of(
                "revenue", jdbc.queryForObject("SELECT COALESCE(SUM(amount),0) FROM payments WHERE company_id=? AND direction='IN' AND paid_at >= DATE_SUB(UTC_TIMESTAMP(), INTERVAL 90 DAY)", java.math.BigDecimal.class, c),
                "expensesByCategory", jdbc.queryForList("SELECT category, ROUND(SUM(amount),2) AS amount FROM expenses WHERE company_id=? AND expense_date >= DATE_SUB(CURDATE(), INTERVAL 90 DAY) GROUP BY category", c),
                "outstandingCustomerInvoices", jdbc.queryForObject("SELECT COALESCE(SUM(total),0) FROM invoices WHERE company_id=? AND kind='CUSTOMER' AND status NOT IN ('PAID','VOID','DRAFT')", java.math.BigDecimal.class, c));
    }
}
