package com.fayzo.fleetpulse.me;

import com.fayzo.fleetpulse.common.ApiException;
import com.fayzo.fleetpulse.common.Authz;
import com.fayzo.fleetpulse.security.AuthPrincipal;
import com.fayzo.fleetpulse.security.Permission;
import com.fayzo.fleetpulse.tenant.TenantContext;
import com.fayzo.fleetpulse.workflow.ShipmentWorkflowService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Own-records-only endpoints for the mobile apps (drivers / employees). The caller is resolved from the JWT to
 * their own driver and employee rows, so a user can never read or change another person's journeys, shipments or salary.
 */
@RestController
@RequestMapping("/api/v1/me")
@Transactional
public class SelfServiceController {
    public record JourneyComplete(@DecimalMin("0") BigDecimal actualDistanceKm, @DecimalMin("0") BigDecimal actualFuelLitres) {}
    public record ShipmentStatus(@NotBlank String status, @Size(max = 300) String note, BigDecimal lat, BigDecimal lng,
                                 @Size(max = 300) String failureReason, @Size(max = 500) String podUrl, @Size(max = 120) String podSignedBy) {}
    public record AttendancePing(BigDecimal lat, BigDecimal lng) {}
    public record FuelEntry(@NotNull @DecimalMin("0.01") BigDecimal litres, @DecimalMin("0") BigDecimal pricePerLitre, @DecimalMin("0") BigDecimal totalCost,
                            @DecimalMin("0") BigDecimal odometerKm, @Size(max = 120) String station, @Size(max = 500) String receiptUrl) {}
    public record DriverExpenseEntry(@NotNull @DecimalMin("0.01") BigDecimal amount, @Size(max = 40) String category, @Size(max = 300) String description, Long journeyId) {}

    private final JdbcTemplate jdbc;
    private final ShipmentWorkflowService shipments;
    public SelfServiceController(JdbcTemplate jdbc, ShipmentWorkflowService shipments) { this.jdbc = jdbc; this.shipments = shipments; }

    private long uid() { return Authz.principal().subjectId(); }
    private Long myDriverId() {
        List<Long> r = jdbc.queryForList("SELECT id FROM drivers WHERE company_id=? AND user_id=?", Long.class, TenantContext.require(), uid());
        return r.isEmpty() ? null : r.get(0);
    }
    private long requireDriver() {
        Long d = myDriverId();
        if (d == null) throw ApiException.forbidden("Your user is not linked to a driver profile");
        return d;
    }
    private Long myEmployeeId() {
        List<Long> r = jdbc.queryForList("SELECT id FROM employees WHERE company_id=? AND user_id=?", Long.class, TenantContext.require(), uid());
        return r.isEmpty() ? null : r.get(0);
    }
    private long requireEmployee() {
        Long e = myEmployeeId();
        if (e == null) throw ApiException.forbidden("Your user is not linked to an employee profile");
        return e;
    }

    @GetMapping("/profile")
    @Transactional(readOnly = true)
    public Map<String, Object> profile() {
        Authz.require(Permission.SELF_SERVICE);
        long c = TenantContext.require();
        List<Map<String, Object>> u = jdbc.queryForList("SELECT u.id, u.full_name AS fullName, u.email, u.mobile, u.role_code AS role, co.name AS company FROM users u JOIN companies co ON co.id=u.company_id WHERE u.id=? AND u.company_id=?", uid(), c);
        Map<String, Object> o = new java.util.LinkedHashMap<>(u.get(0));
        o.put("driverId", myDriverId());
        o.put("employeeId", myEmployeeId());
        Long d = myDriverId();
        o.put("assignedVehicle", d == null ? null : jdbc.queryForList("SELECT id, registration_number AS registrationNumber, make, model FROM vehicles WHERE company_id=? AND assigned_driver_id=? LIMIT 1", c, d).stream().findFirst().orElse(null));
        return o;
    }

    // ---- Journeys ----
    @GetMapping("/journeys")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> journeys() {
        Authz.require(Permission.JOURNEY_SELF);
        return jdbc.queryForList("""
            SELECT id, journey_code AS journeyCode, start_location AS startLocation, destination, planned_start AS plannedStart, planned_end AS plannedEnd,
                   actual_start AS actualStart, actual_end AS actualEnd, planned_distance_km AS plannedDistanceKm, eta_at AS etaAt, status, vehicle_id AS vehicleId
            FROM journeys WHERE company_id=? AND driver_id=? ORDER BY id DESC LIMIT 100""", TenantContext.require(), requireDriver());
    }

    @PostMapping("/journeys/{id}/start")
    public Map<String, Boolean> startJourney(@PathVariable long id) {
        Authz.require(Permission.JOURNEY_SELF);
        long c = TenantContext.require(), d = requireDriver();
        int n = jdbc.update("UPDATE journeys SET status='IN_PROGRESS', actual_start=UTC_TIMESTAMP(3) WHERE id=? AND company_id=? AND driver_id=? AND status='PLANNED'", id, c, d);
        if (n == 0) throw ApiException.badRequest("CANNOT_START", "Journey not found or not in PLANNED state");
        jdbc.update("UPDATE vehicles SET status='IN_USE' WHERE company_id=? AND id=(SELECT vehicle_id FROM journeys WHERE id=?) AND status='AVAILABLE'", c, id);
        jdbc.update("UPDATE drivers SET status='ON_TRIP' WHERE id=? AND company_id=?", d, c);
        return Map.of("ok", true);
    }

    @PostMapping("/journeys/{id}/complete")
    public Map<String, Boolean> completeJourney(@PathVariable long id, @Valid @RequestBody(required = false) JourneyComplete b) {
        Authz.require(Permission.JOURNEY_SELF);
        long c = TenantContext.require(), d = requireDriver();
        int n = jdbc.update("UPDATE journeys SET status='COMPLETED', actual_end=UTC_TIMESTAMP(3), actual_distance_km=COALESCE(?,actual_distance_km), actual_fuel_litres=COALESCE(?,actual_fuel_litres) WHERE id=? AND company_id=? AND driver_id=? AND status='IN_PROGRESS'",
                b == null ? null : b.actualDistanceKm(), b == null ? null : b.actualFuelLitres(), id, c, d);
        if (n == 0) throw ApiException.badRequest("CANNOT_COMPLETE", "Journey not found or not in progress");
        jdbc.update("UPDATE vehicles SET status='AVAILABLE' WHERE company_id=? AND id=(SELECT vehicle_id FROM journeys WHERE id=?) AND status='IN_USE'", c, id);
        jdbc.update("UPDATE drivers SET status='AVAILABLE' WHERE id=? AND company_id=?", d, c);
        // distance-based earnings for work-based staff: one pending record per journey, approved later by a manager
        if (b != null && b.actualDistanceKm() != null) {
            List<Map<String, Object>> e = jdbc.queryForList("SELECT e.id, ss.per_trip_rate, ss.per_km_rate FROM employees e JOIN salary_structures ss ON ss.employee_id=e.id AND ss.company_id=e.company_id WHERE e.company_id=? AND e.driver_id=? AND ss.model='WORK_BASED'", c, d);
            if (!e.isEmpty()) {
                BigDecimal tripRate = (BigDecimal) e.get(0).get("per_trip_rate"), kmRate = (BigDecimal) e.get(0).get("per_km_rate");
                if (tripRate != null) jdbc.update("INSERT INTO work_records(company_id,employee_id,work_type,reference_id,quantity,rate,amount,work_date,status) VALUES (?,?,'TRIP',?,1,?,?,CURDATE(),'PENDING')", c, e.get(0).get("id"), id, tripRate, tripRate);
                if (kmRate != null) jdbc.update("INSERT INTO work_records(company_id,employee_id,work_type,reference_id,quantity,rate,amount,work_date,status) VALUES (?,?,'DISTANCE',?,?,?,?,CURDATE(),'PENDING')", c, e.get(0).get("id"), id, b.actualDistanceKm(), kmRate, kmRate.multiply(b.actualDistanceKm()));
            }
        }
        return Map.of("ok", true);
    }

    // ---- Shipments ----
    @GetMapping("/shipments")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> myShipments() {
        Authz.require(Permission.SHIPMENT_SELF);
        return jdbc.queryForList("""
            SELECT id, tracking_id AS trackingId, status, pickup_address AS pickupAddress, pickup_lat AS pickupLat, pickup_lng AS pickupLng,
                   destination_address AS destinationAddress, dest_lat AS destLat, dest_lng AS destLng, receiver_name AS receiverName, receiver_mobile AS receiverMobile,
                   package_description AS packageDescription, package_count AS packageCount, scheduled_delivery AS scheduledDelivery
            FROM shipments WHERE company_id=? AND driver_id=? AND status NOT IN ('CANCELLED') ORDER BY id DESC LIMIT 200""", TenantContext.require(), requireDriver());
    }

    @PostMapping("/shipments/{id}/status")
    public Map<String, Object> shipmentStatus(@PathVariable long id, @Valid @RequestBody ShipmentStatus b) {
        Authz.require(Permission.SHIPMENT_SELF);
        return shipments.change(id, new ShipmentWorkflowService.StatusChange(b.status(), b.note(), b.lat(), b.lng(), b.failureReason(), b.podUrl(), b.podSignedBy()), requireDriver());
    }

    /** Resolves a scanned QR/barcode (tracking ID) to one of MY shipments only. */
    @GetMapping("/shipments/scan/{code}")
    @Transactional(readOnly = true)
    public Map<String, Object> scan(@PathVariable String code) {
        Authz.require(Permission.SHIPMENT_SELF);
        List<Map<String, Object>> r = jdbc.queryForList("SELECT id, tracking_id AS trackingId, status, destination_address AS destinationAddress FROM shipments WHERE company_id=? AND driver_id=? AND tracking_id=?", TenantContext.require(), requireDriver(), code.trim().toUpperCase());
        if (r.isEmpty()) throw ApiException.notFound("No shipment with this code is assigned to you");
        return r.get(0);
    }

    // ---- Attendance ----
    @PostMapping("/attendance/check-in")
    public Map<String, Boolean> checkIn(@RequestBody(required = false) AttendancePing p) {
        Authz.require(Permission.ATTENDANCE_SELF);
        long c = TenantContext.require(), e = requireEmployee();
        try {
            jdbc.update("INSERT INTO attendance(company_id,employee_id,work_date,check_in,status,lat,lng) VALUES (?,?,CURDATE(),UTC_TIMESTAMP(3),'PRESENT',?,?)", c, e, p == null ? null : p.lat(), p == null ? null : p.lng());
        } catch (org.springframework.dao.DuplicateKeyException ex) { throw ApiException.conflict("ALREADY_CHECKED_IN", "You already checked in today"); }
        return Map.of("ok", true);
    }

    @PostMapping("/attendance/check-out")
    public Map<String, Boolean> checkOut() {
        Authz.require(Permission.ATTENDANCE_SELF);
        int n = jdbc.update("UPDATE attendance SET check_out=UTC_TIMESTAMP(3) WHERE company_id=? AND employee_id=? AND work_date=CURDATE() AND check_in IS NOT NULL AND check_out IS NULL", TenantContext.require(), requireEmployee());
        if (n == 0) throw ApiException.badRequest("NOT_CHECKED_IN", "No open check-in for today");
        return Map.of("ok", true);
    }

    @GetMapping("/attendance")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> attendance() {
        Authz.require(Permission.ATTENDANCE_SELF);
        return jdbc.queryForList("SELECT work_date AS workDate, check_in AS checkIn, check_out AS checkOut, status FROM attendance WHERE company_id=? AND employee_id=? ORDER BY work_date DESC LIMIT 62", TenantContext.require(), requireEmployee());
    }

    // ---- Salary and earnings: strictly the caller's own ----
    @GetMapping("/payslips")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> payslips() {
        Authz.require(Permission.SELF_SERVICE);
        return jdbc.queryForList("""
            SELECT id, period_month AS periodMonth, model, basic, overtime, bonus, deductions, advances, work_earnings AS workEarnings, net_salary AS netSalary,
                   working_days AS workingDays, present_days AS presentDays, leave_days AS leaveDays, status, paid_on AS paidOn
            FROM payslips WHERE company_id=? AND employee_id=? AND status IN ('FINALIZED','PAID') ORDER BY period_month DESC LIMIT 36""", TenantContext.require(), requireEmployee());
    }

    @GetMapping("/earnings")
    @Transactional(readOnly = true)
    public Map<String, Object> earnings() {
        Authz.require(Permission.SELF_SERVICE);
        long c = TenantContext.require(), e = requireEmployee();
        Map<String, Object> o = new java.util.LinkedHashMap<>();
        o.put("records", jdbc.queryForList("SELECT id, work_type AS workType, quantity, rate, amount, work_date AS workDate, status FROM work_records WHERE company_id=? AND employee_id=? ORDER BY id DESC LIMIT 200", c, e));
        o.put("pendingApproval", jdbc.queryForObject("SELECT COALESCE(SUM(amount),0) FROM work_records WHERE company_id=? AND employee_id=? AND status='PENDING'", BigDecimal.class, c, e));
        o.put("approvedUnpaid", jdbc.queryForObject("SELECT COALESCE(SUM(amount),0) FROM work_records WHERE company_id=? AND employee_id=? AND status='APPROVED'", BigDecimal.class, c, e));
        o.put("paidTotal", jdbc.queryForObject("SELECT COALESCE(SUM(amount),0) FROM work_records WHERE company_id=? AND employee_id=? AND status='PAID'", BigDecimal.class, c, e));
        return o;
    }

    // ---- Fuel and expenses (driver) ----
    @PostMapping("/fuel")
    public Map<String, Boolean> addFuel(@Valid @RequestBody FuelEntry f) {
        Authz.require(Permission.FUEL_SELF);
        long c = TenantContext.require(), d = requireDriver();
        List<Long> v = jdbc.queryForList("SELECT id FROM vehicles WHERE company_id=? AND assigned_driver_id=? LIMIT 1", Long.class, c, d);
        if (v.isEmpty()) throw ApiException.badRequest("NO_VEHICLE", "No vehicle is assigned to you");
        BigDecimal total = f.totalCost() != null ? f.totalCost() : f.pricePerLitre() != null ? f.litres().multiply(f.pricePerLitre()) : null;
        if (total == null) throw ApiException.badRequest("COST_REQUIRED", "Provide totalCost or pricePerLitre");
        jdbc.update("INSERT INTO fuel_records(company_id,vehicle_id,driver_id,fuel_date,litres,price_per_litre,total_cost,odometer_km,station,receipt_url) VALUES (?,?,?,?,?,?,?,?,?,?)",
                c, v.get(0), d, java.sql.Timestamp.from(Instant.now()), f.litres(), f.pricePerLitre(), total, f.odometerKm(), f.station(), f.receiptUrl());
        return Map.of("ok", true);
    }

    @PostMapping("/expenses")
    public Map<String, Boolean> addExpense(@Valid @RequestBody DriverExpenseEntry x) {
        Authz.require(Permission.FUEL_SELF);
        long c = TenantContext.require(), d = requireDriver();
        if (x.journeyId() != null) {
            Integer ok = jdbc.queryForObject("SELECT COUNT(*) FROM journeys WHERE id=? AND company_id=? AND driver_id=?", Integer.class, x.journeyId(), c, d);
            if (ok == null || ok == 0) throw ApiException.forbidden("This journey is not assigned to you");
        }
        jdbc.update("INSERT INTO driver_expenses(company_id,driver_id,journey_id,expense_date,category,amount,description) VALUES (?,?,?,CURDATE(),?,?,?)", c, d, x.journeyId(), x.category(), x.amount(), x.description());
        return Map.of("ok", true);
    }
}
