package com.fayzo.fleetpulse.routing;

import com.fayzo.fleetpulse.common.ApiException;
import com.fayzo.fleetpulse.common.Authz;
import com.fayzo.fleetpulse.routing.RoutingClient.Point;
import com.fayzo.fleetpulse.routing.RoutingClient.Route;
import com.fayzo.fleetpulse.security.Permission;
import com.fayzo.fleetpulse.tenant.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

/**
 * Route planning and multi-stop optimisation on real road networks (OpenRouteService). Results are returned to the client, which can
 * save them with POST /api/v1/routes. Fuel estimates use the vehicle's own measured consumption; if there is not enough fuel/odometer
 * history the estimate is null with a reason, never a guess. Toll cost is only estimated when the caller provides a rate per km.
 */
@RestController
@RequestMapping("/api/v1/route-planning")
public class RouteController {
    public record Pt(@NotNull @DecimalMin("-90") @DecimalMax("90") Double lat, @NotNull @DecimalMin("-180") @DecimalMax("180") Double lng) {}
    public record PlanRequest(@NotNull @Valid Pt origin, @NotNull @Valid Pt destination, @Size(max = 20) List<@Valid Pt> stops,
                              @NotBlank @Pattern(regexp = "FASTEST|SHORTEST|FUEL_EFFICIENT") String mode, Long vehicleId, @DecimalMin("0") BigDecimal tollRatePerKm) {}
    public record OptimizeRequest(@NotNull @Valid Pt origin, @NotNull @Size(min = 2, max = 25) List<@Valid Pt> stops, boolean returnToOrigin) {}

    private final RoutingClient routing;
    private final JdbcTemplate jdbc;
    public RouteController(RoutingClient routing, JdbcTemplate jdbc) { this.routing = routing; this.jdbc = jdbc; }

    @PostMapping("/plan")
    public Map<String, Object> plan(@Valid @RequestBody PlanRequest r) {
        Authz.require(Permission.ROUTE_READ);
        List<Point> pts = new ArrayList<>();
        pts.add(new Point(r.origin().lat(), r.origin().lng()));
        if (r.stops() != null) r.stops().forEach(s -> pts.add(new Point(s.lat(), s.lng())));
        pts.add(new Point(r.destination().lat(), r.destination().lng()));

        Route route; String note = null;
        switch (r.mode()) {
            case "FASTEST" -> route = routing.directions(pts, "fastest");
            case "SHORTEST" -> route = routing.directions(pts, "shortest");
            default -> {
                // No road-network provider optimises fuel directly. Approximation: take the shorter route unless it is >15% slower than the fastest.
                Route fast = routing.directions(pts, "fastest"), shortest = routing.directions(pts, "shortest");
                boolean useShort = shortest.distanceMeters() < fast.distanceMeters() && shortest.durationSeconds() <= fast.durationSeconds() * 1.15;
                route = useShort ? shortest : fast;
                note = "Fuel-efficient mode is an approximation: it prefers the shorter route when it is no more than 15% slower than the fastest.";
            }
        }
        double km = route.distanceMeters() / 1000.0;
        Map<String, Object> o = new LinkedHashMap<>();
        o.put("mode", r.mode());
        o.put("distanceKm", round(km));
        o.put("durationMinutes", Math.round(route.durationSeconds() / 60.0));
        o.put("etaAt", java.time.Instant.now().plusSeconds((long) route.durationSeconds()).toString());
        o.put("tollRoadKm", round(route.tollRoadMeters() / 1000.0));
        o.put("tollEstimate", r.tollRatePerKm() == null ? null : r.tollRatePerKm().multiply(BigDecimal.valueOf(route.tollRoadMeters() / 1000.0)).setScale(2, java.math.RoundingMode.HALF_UP));
        o.put("tollEstimateNote", r.tollRatePerKm() == null ? "Toll prices are not available from the routing data; provide tollRatePerKm to estimate." : "Estimated from your rate per km on toll roads.");
        Double lPerKm = r.vehicleId() == null ? null : litresPerKm(r.vehicleId());
        o.put("fuelEstimateLitres", lPerKm == null ? null : round(km * lPerKm));
        o.put("fuelEstimateNote", r.vehicleId() == null ? "Select a vehicle for a fuel estimate." : lPerKm == null ? "Not enough fuel and odometer history for this vehicle." : "Based on this vehicle's measured consumption over the last 180 days.");
        o.put("geometry", simplify(route.coordinates(), 400));
        if (note != null) o.put("note", note);
        return o;
    }

    /** Orders stops to minimise total driving time (nearest neighbour + 2-opt on a real road-time matrix). The first point is the origin. */
    @PostMapping("/optimize")
    public Map<String, Object> optimize(@Valid @RequestBody OptimizeRequest r) {
        Authz.require(Permission.ROUTE_READ);
        List<Point> pts = new ArrayList<>();
        pts.add(new Point(r.origin().lat(), r.origin().lng()));
        r.stops().forEach(s -> pts.add(new Point(s.lat(), s.lng())));
        double[][] m = routing.durationMatrix(pts);
        int n = pts.size();
        List<Integer> order = new ArrayList<>(); boolean[] used = new boolean[n];
        order.add(0); used[0] = true;
        for (int i = 1; i < n; i++) {
            int last = order.get(order.size() - 1), best = -1;
            for (int j = 1; j < n; j++) if (!used[j] && (best < 0 || m[last][j] < m[last][best])) best = j;
            order.add(best); used[best] = true;
        }
        boolean improved = true;
        while (improved) {
            improved = false;
            for (int i = 1; i < n - 1; i++) for (int k = i + 1; k < n; k++) {
                List<Integer> cand = new ArrayList<>(order);
                Collections.reverse(cand.subList(i, k + 1));
                if (cost(cand, m, r.returnToOrigin()) + 1e-6 < cost(order, m, r.returnToOrigin())) { order = cand; improved = true; }
            }
        }
        List<Integer> stopOrder = order.subList(1, n).stream().map(i -> i - 1).toList();   // indexes into the request's stops
        double before = cost(identity(n), m, r.returnToOrigin()), after = cost(order, m, r.returnToOrigin());
        return Map.of("stopOrder", stopOrder, "totalDriveMinutesOptimized", Math.round(after / 60), "totalDriveMinutesAsGiven", Math.round(before / 60),
                "method", "nearest neighbour + 2-opt over road-network travel times");
    }

    private static List<Integer> identity(int n) { List<Integer> l = new ArrayList<>(); for (int i = 0; i < n; i++) l.add(i); return l; }
    private static double cost(List<Integer> o, double[][] m, boolean ret) {
        double c = 0;
        for (int i = 0; i + 1 < o.size(); i++) c += m[o.get(i)][o.get(i + 1)];
        if (ret) c += m[o.get(o.size() - 1)][o.get(0)];
        return c;
    }

    private Double litresPerKm(long vehicleId) {
        List<Map<String, Object>> recs = jdbc.queryForList("SELECT litres, odometer_km FROM fuel_records WHERE company_id=? AND vehicle_id=? AND odometer_km IS NOT NULL AND fuel_date >= DATE_SUB(UTC_TIMESTAMP(), INTERVAL 180 DAY) ORDER BY odometer_km",
                TenantContext.require(), vehicleId);
        if (recs.size() < 3) return null;
        double dist = ((BigDecimal) recs.get(recs.size() - 1).get("odometer_km")).subtract((BigDecimal) recs.get(0).get("odometer_km")).doubleValue();
        double litres = 0;
        for (int i = 1; i < recs.size(); i++) litres += ((BigDecimal) recs.get(i).get("litres")).doubleValue();
        return dist > 50 && litres > 0 ? litres / dist : null;
    }

    private static List<double[]> simplify(List<double[]> line, int max) {
        if (line.size() <= max) return line;
        List<double[]> out = new ArrayList<>(); double step = (line.size() - 1) / (double) (max - 1);
        for (int i = 0; i < max; i++) out.add(line.get((int) Math.round(i * step)));
        return out;
    }
    private static double round(double v) { return Math.round(v * 100.0) / 100.0; }
}
