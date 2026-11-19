package com.fayzo.fleetpulse.gps;

import com.fayzo.fleetpulse.common.ApiException;
import com.fayzo.fleetpulse.common.Authz;
import com.fayzo.fleetpulse.security.AuthPrincipal;
import com.fayzo.fleetpulse.security.Permission;
import com.fayzo.fleetpulse.tenant.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * GPS positions come ONLY from authenticated driver-app posts (or registered GPS hardware later).
 * Nothing here generates or simulates coordinates; a vehicle without recent reports is reported as having no live location.
 */
@RestController
@RequestMapping("/api/v1/gps")
@Transactional
public class GpsController {
    public static final int STALE_MINUTES = 10;

    public record LocationPing(
            @NotNull @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
            @NotNull @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
            @DecimalMin("0") @DecimalMax("400") BigDecimal speedKmh,
            @DecimalMin("0") @DecimalMax("360") BigDecimal headingDeg,
            @DecimalMin("0") BigDecimal accuracyM,
            Instant recordedAt, Long vehicleId, Long journeyId) {}

    private final JdbcTemplate jdbc;
    private final GpsQuota quota;
    public GpsController(JdbcTemplate jdbc, GpsQuota quota) { this.jdbc = jdbc; this.quota = quota; }

    /** Driver app: publish own position. The vehicle must be the one assigned to the calling driver. */
    @PostMapping("/locations")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Boolean> publish(@Valid @RequestBody LocationPing p) {
        Authz.require(Permission.GPS_PUBLISH);
        AuthPrincipal me = Authz.principal();
        long c = TenantContext.require();
        quota.check(c);
        List<Map<String, Object>> drv = jdbc.queryForList("SELECT id FROM drivers WHERE company_id=? AND user_id=?", c, me.subjectId());
        if (drv.isEmpty()) throw ApiException.forbidden("Your user is not linked to a driver profile");
        long driverId = ((Number) drv.get(0).get("id")).longValue();
        Long vehicleId = p.vehicleId();
        if (vehicleId == null) {
            List<Long> v = jdbc.queryForList("SELECT id FROM vehicles WHERE company_id=? AND assigned_driver_id=? LIMIT 1", Long.class, c, driverId);
            vehicleId = v.isEmpty() ? null : v.get(0);
        } else {
            Integer ok = jdbc.queryForObject("SELECT COUNT(*) FROM vehicles WHERE id=? AND company_id=? AND assigned_driver_id=?", Integer.class, vehicleId, c, driverId);
            if (ok == null || ok == 0) throw ApiException.forbidden("This vehicle is not assigned to you");
        }
        if (p.journeyId() != null) {
            Integer ok = jdbc.queryForObject("SELECT COUNT(*) FROM journeys WHERE id=? AND company_id=? AND driver_id=?", Integer.class, p.journeyId(), c, driverId);
            if (ok == null || ok == 0) throw ApiException.forbidden("This journey is not assigned to you");
        }
        Instant at = p.recordedAt() == null ? Instant.now() : p.recordedAt();
        if (at.isAfter(Instant.now().plusSeconds(300))) throw ApiException.badRequest("BAD_TIME", "recordedAt is in the future");
        jdbc.update("""
            INSERT INTO gps_locations(company_id,vehicle_id,driver_id,journey_id,latitude,longitude,speed_kmh,heading_deg,accuracy_m,source,recorded_at)
            VALUES (?,?,?,?,?,?,?,?,?,'DRIVER_APP',?)""",
                c, vehicleId, driverId, p.journeyId(), p.latitude(), p.longitude(), p.speedKmh(), p.headingDeg(), p.accuracyM(), Timestamp.from(at));
        return Map.of("ok", true);
    }

    /** Latest real report per vehicle. Vehicles with no report (or a stale one) are flagged, never filled in. */
    @GetMapping("/live")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> live() {
        Authz.require(Permission.GPS_READ);
        long c = TenantContext.require();
        return jdbc.queryForList("""
            SELECT v.id AS vehicleId, v.registration_number AS registrationNumber, v.status AS vehicleStatus,
                   g.latitude, g.longitude, g.speed_kmh AS speedKmh, g.heading_deg AS headingDeg, g.recorded_at AS recordedAt, g.driver_id AS driverId,
                   (g.recorded_at IS NULL OR g.recorded_at < DATE_SUB(UTC_TIMESTAMP(3), INTERVAL ? MINUTE)) AS stale
            FROM vehicles v
            LEFT JOIN (SELECT vehicle_id, MAX(recorded_at) m FROM gps_locations WHERE company_id=? AND vehicle_id IS NOT NULL GROUP BY vehicle_id) x ON x.vehicle_id=v.id
            LEFT JOIN gps_locations g ON g.vehicle_id=x.vehicle_id AND g.recorded_at=x.m AND g.company_id=?
            WHERE v.company_id=? AND v.status NOT IN ('SOLD','RETIRED')""", STALE_MINUTES, c, c, c);
    }

    @GetMapping("/history")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> history(@RequestParam long vehicleId, @RequestParam Instant from, @RequestParam Instant to) {
        Authz.require(Permission.GPS_READ);
        if (to.isBefore(from) || java.time.Duration.between(from, to).toDays() > 31) throw ApiException.badRequest("BAD_RANGE", "Range must be at most 31 days");
        return jdbc.queryForList("""
            SELECT latitude, longitude, speed_kmh AS speedKmh, heading_deg AS headingDeg, recorded_at AS recordedAt
            FROM gps_locations WHERE company_id=? AND vehicle_id=? AND recorded_at BETWEEN ? AND ? ORDER BY recorded_at LIMIT 5000""",
                TenantContext.require(), vehicleId, Timestamp.from(from), Timestamp.from(to));
    }

    /** Pings recorded outside every active geofence while a journey is running (unauthorised-stop / deviation review feed). */
    @GetMapping("/geofence-events")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> geofenceExits(@RequestParam long vehicleId) {
        Authz.require(Permission.GPS_READ);
        long c = TenantContext.require();
        return jdbc.queryForList("""
            SELECT g.recorded_at AS recordedAt, g.latitude, g.longitude, f.name AS geofence,
                   (6371000 * ACOS(LEAST(1, COS(RADIANS(f.center_lat))*COS(RADIANS(g.latitude))*COS(RADIANS(g.longitude)-RADIANS(f.center_lng)) + SIN(RADIANS(f.center_lat))*SIN(RADIANS(g.latitude))))) AS distanceM
            FROM gps_locations g JOIN geofences f ON f.company_id=g.company_id AND f.active=TRUE
            WHERE g.company_id=? AND g.vehicle_id=? AND g.recorded_at > DATE_SUB(UTC_TIMESTAMP(), INTERVAL 7 DAY)
            HAVING distanceM > (SELECT radius_meters FROM geofences WHERE id=f.id)
            ORDER BY g.recorded_at DESC LIMIT 500""", c, vehicleId);
    }
}
