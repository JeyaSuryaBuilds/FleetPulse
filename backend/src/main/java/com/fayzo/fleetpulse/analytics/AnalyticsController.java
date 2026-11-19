package com.fayzo.fleetpulse.analytics;

import com.fayzo.fleetpulse.common.ApiException;
import com.fayzo.fleetpulse.common.Authz;
import com.fayzo.fleetpulse.security.Permission;
import com.fayzo.fleetpulse.tenant.TenantContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Calculated fleet analytics. All figures derive from recorded data; vehicles without the needed inputs are reported as such, never estimated. */
@RestController
@RequestMapping("/api/v1/analytics")
@Transactional(readOnly = true)
public class AnalyticsController {
    private final JdbcTemplate jdbc;
    public AnalyticsController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    /** Straight-line depreciation: (price - residual) / useful life, per year, pro-rated by days owned. Resale gain/loss uses book value at the sale date. */
    @GetMapping("/depreciation")
    public List<Map<String, Object>> depreciation() {
        Authz.require(Permission.FLEET_READ);
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> v : jdbc.queryForList("""
            SELECT id, registration_number, purchase_date, purchase_price, residual_value, useful_life_years, sale_date, sale_price, status
            FROM vehicles WHERE company_id=? ORDER BY registration_number""", TenantContext.require())) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("vehicleId", v.get("id"));
            r.put("registrationNumber", v.get("registration_number"));
            BigDecimal price = (BigDecimal) v.get("purchase_price");
            Date pd = (Date) v.get("purchase_date");
            Integer life = (Integer) v.get("useful_life_years");
            if (price == null || pd == null || life == null || life <= 0) {
                r.put("calculable", false);
                r.put("missing", "purchase_price, purchase_date and useful_life_years are required");
                out.add(r); continue;
            }
            BigDecimal residual = v.get("residual_value") == null ? BigDecimal.ZERO : (BigDecimal) v.get("residual_value");
            BigDecimal annual = price.subtract(residual).divide(BigDecimal.valueOf(life), 2, RoundingMode.HALF_UP);
            LocalDate end = v.get("sale_date") != null ? ((Date) v.get("sale_date")).toLocalDate() : LocalDate.now();
            r.put("calculable", true);
            r.put("annualDepreciation", annual);
            r.put("accumulatedDepreciation", accumulated(price, residual, life, pd.toLocalDate(), LocalDate.now()));
            r.put("currentBookValue", price.subtract(accumulated(price, residual, life, pd.toLocalDate(), LocalDate.now())));
            if (v.get("sale_date") != null && v.get("sale_price") != null) {
                BigDecimal bookAtSale = price.subtract(accumulated(price, residual, life, pd.toLocalDate(), end));
                r.put("bookValueAtSale", bookAtSale);
                r.put("resaleGainLoss", ((BigDecimal) v.get("sale_price")).subtract(bookAtSale));
            }
            out.add(r);
        }
        return out;
    }

    private static BigDecimal accumulated(BigDecimal price, BigDecimal residual, int lifeYears, LocalDate from, LocalDate to) {
        long days = Math.max(0, ChronoUnit.DAYS.between(from, to));
        BigDecimal total = price.subtract(residual).max(BigDecimal.ZERO);
        BigDecimal part = total.multiply(BigDecimal.valueOf(days)).divide(BigDecimal.valueOf(lifeYears * 365L), 2, RoundingMode.HALF_UP);
        return part.min(total);
    }

    /** Utilisation = days with at least one started journey / days in range. Distance from completed journeys. */
    @GetMapping("/utilization")
    public Map<String, Object> utilization(@RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to) {
        Authz.require(Permission.FLEET_READ);
        LocalDate f = from == null ? LocalDate.now().minusDays(30) : from, t = to == null ? LocalDate.now() : to;
        long days = ChronoUnit.DAYS.between(f, t) + 1;
        if (days < 1 || days > 366) throw ApiException.badRequest("BAD_RANGE", "Range must be 1 to 366 days");
        List<Map<String, Object>> rows = jdbc.queryForList("""
            SELECT v.id AS vehicleId, v.registration_number AS registrationNumber,
              (SELECT COUNT(DISTINCT DATE(j.actual_start)) FROM journeys j WHERE j.vehicle_id=v.id AND j.company_id=v.company_id AND j.actual_start>=? AND j.actual_start<DATE_ADD(?, INTERVAL 1 DAY)) AS activeDays,
              (SELECT COALESCE(SUM(j.actual_distance_km),0) FROM journeys j WHERE j.vehicle_id=v.id AND j.company_id=v.company_id AND j.status='COMPLETED' AND j.actual_end>=? AND j.actual_end<DATE_ADD(?, INTERVAL 1 DAY)) AS distanceKm
            FROM vehicles v WHERE v.company_id=? AND v.status NOT IN ('SOLD','RETIRED') ORDER BY v.registration_number""", f, t, f, t, TenantContext.require());
        for (Map<String, Object> r : rows) r.put("utilizationPercent", BigDecimal.valueOf(((Number) r.get("activeDays")).doubleValue() * 100.0 / days).setScale(1, RoundingMode.HALF_UP));
        OptionalDouble avg = rows.stream().mapToDouble(r -> ((BigDecimal) r.get("utilizationPercent")).doubleValue()).average();
        return Map.of("from", f, "to", t, "days", days, "definition", "activeDays / days in range", "fleetAveragePercent", avg.isPresent() ? Math.round(avg.getAsDouble() * 10) / 10.0 : 0, "vehicles", rows);
    }

    /**
     * Fuel efficiency from consecutive fill-ups (full-tank method: distance between odometer readings / litres of the later fill-up).
     * A fill-up is flagged unusual when its efficiency is more than 30% worse than that vehicle's own average in the range.
     * Needs odometer readings; vehicles without at least 3 usable readings are listed under insufficientData.
     */
    @GetMapping("/fuel-efficiency")
    public Map<String, Object> fuelEfficiency(@RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to) {
        Authz.require(Permission.FUEL_READ);
        LocalDate f = from == null ? LocalDate.now().minusDays(90) : from, t = to == null ? LocalDate.now() : to;
        List<Map<String, Object>> recs = jdbc.queryForList("""
            SELECT f.vehicle_id, v.registration_number AS reg, f.id, f.fuel_date, f.litres, f.odometer_km
            FROM fuel_records f JOIN vehicles v ON v.id=f.vehicle_id AND v.company_id=f.company_id
            WHERE f.company_id=? AND f.fuel_date>=? AND f.fuel_date<DATE_ADD(?, INTERVAL 1 DAY) AND f.odometer_km IS NOT NULL
            ORDER BY f.vehicle_id, f.odometer_km""", TenantContext.require(), f, t);
        Map<Long, List<Map<String, Object>>> byVehicle = new LinkedHashMap<>();
        for (var r : recs) byVehicle.computeIfAbsent(((Number) r.get("vehicle_id")).longValue(), k -> new ArrayList<>()).add(r);

        List<Map<String, Object>> vehicles = new ArrayList<>(), anomalies = new ArrayList<>(), insufficient = new ArrayList<>();
        for (var e : byVehicle.entrySet()) {
            List<Map<String, Object>> list = e.getValue();
            String reg = (String) list.get(0).get("reg");
            List<double[]> pairs = new ArrayList<>(); List<Map<String, Object>> src = new ArrayList<>();
            for (int i = 1; i < list.size(); i++) {
                double dist = ((BigDecimal) list.get(i).get("odometer_km")).subtract((BigDecimal) list.get(i - 1).get("odometer_km")).doubleValue();
                double litres = ((BigDecimal) list.get(i).get("litres")).doubleValue();
                if (dist > 0 && litres > 0) { pairs.add(new double[]{dist, litres, dist / litres}); src.add(list.get(i)); }
            }
            if (pairs.size() < 2) { insufficient.add(Map.of("vehicleId", e.getKey(), "registrationNumber", reg)); continue; }
            double dist = pairs.stream().mapToDouble(p -> p[0]).sum(), litres = pairs.stream().mapToDouble(p -> p[1]).sum(), avg = dist / litres;
            Map<String, Object> v = new LinkedHashMap<>();
            v.put("vehicleId", e.getKey()); v.put("registrationNumber", reg);
            v.put("distanceKm", round(dist)); v.put("litres", round(litres)); v.put("kmPerLitre", round(avg)); v.put("litresPer100Km", round(100 / avg));
            vehicles.add(v);
            for (int i = 0; i < pairs.size(); i++) {
                if (pairs.get(i)[2] < avg * 0.7)
                    anomalies.add(Map.of("vehicleId", e.getKey(), "registrationNumber", reg, "fuelRecordId", src.get(i).get("id"), "fuelDate", String.valueOf(src.get(i).get("fuel_date")),
                            "kmPerLitre", round(pairs.get(i)[2]), "vehicleAverageKmPerLitre", round(avg), "reason", "More than 30% worse than this vehicle's average (possible leak, theft, idling or a missed fill-up reading)"));
            }
        }
        return Map.of("from", f, "to", t, "method", "full-tank odometer method", "vehicles", vehicles, "unusualConsumption", anomalies, "insufficientData", insufficient);
    }

    private static double round(double v) { return Math.round(v * 100.0) / 100.0; }
}
