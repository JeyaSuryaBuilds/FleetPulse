package com.fayzo.fleetpulse.gps;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fayzo.fleetpulse.common.ApiException;
import com.fayzo.fleetpulse.common.Authz;
import com.fayzo.fleetpulse.routing.Geo;
import com.fayzo.fleetpulse.security.Permission;
import com.fayzo.fleetpulse.tenant.TenantContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

/**
 * Route deviation and unauthorised-stop detection from REAL recorded pings of a journey.
 * Deviation needs a saved route (routes.waypoints_json = JSON array of [lat,lng]); without one it says so instead of guessing.
 * A stop = pings with speed <= 2 km/h spanning at least minStopMinutes; it is "unauthorised" when not within stopRadiusM of the journey's
 * start, destination or planned stops.
 */
@RestController
@RequestMapping("/api/v1/gps/journeys")
@Transactional(readOnly = true)
public class JourneyMonitorController {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    public JourneyMonitorController(JdbcTemplate jdbc, ObjectMapper mapper) { this.jdbc = jdbc; this.mapper = mapper; }

    @GetMapping("/{journeyId}/monitor")
    public Map<String, Object> monitor(@PathVariable long journeyId, @RequestParam(defaultValue = "500") int deviationMeters,
                                       @RequestParam(defaultValue = "15") int minStopMinutes, @RequestParam(defaultValue = "300") int stopRadiusM) {
        Authz.require(Permission.GPS_READ);
        long c = TenantContext.require();
        List<Map<String, Object>> j = jdbc.queryForList("SELECT start_lat, start_lng, dest_lat, dest_lng FROM journeys WHERE id=? AND company_id=?", journeyId, c);
        if (j.isEmpty()) throw ApiException.notFound("Journey not found");
        List<Map<String, Object>> pings = jdbc.queryForList("SELECT latitude, longitude, speed_kmh, recorded_at FROM gps_locations WHERE company_id=? AND journey_id=? ORDER BY recorded_at LIMIT 20000", c, journeyId);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("pingCount", pings.size());
        if (pings.isEmpty()) { out.put("message", "No GPS data has been recorded for this journey."); out.put("deviations", List.of()); out.put("unauthorizedStops", List.of()); return out; }

        // deviation
        List<Map<String, Object>> routeRows = jdbc.queryForList("SELECT waypoints_json FROM routes WHERE company_id=? AND journey_id=? AND waypoints_json IS NOT NULL ORDER BY id DESC LIMIT 1", c, journeyId);
        List<Map<String, Object>> deviations = new ArrayList<>();
        if (routeRows.isEmpty()) {
            out.put("deviationStatus", "No saved route with geometry for this journey, so deviation cannot be assessed.");
        } else {
            List<double[]> line = new ArrayList<>();
            try { for (double[] p : mapper.readValue((String) routeRows.get(0).get("waypoints_json"), double[][].class)) line.add(p); }
            catch (Exception e) { out.put("deviationStatus", "Saved route geometry could not be read."); }
            if (!line.isEmpty()) {
                out.put("deviationStatus", "OK");
                for (var p : pings) {
                    double lat = ((BigDecimal) p.get("latitude")).doubleValue(), lng = ((BigDecimal) p.get("longitude")).doubleValue();
                    double dist = Geo.distanceToPolyline(lat, lng, line);
                    if (dist > deviationMeters) deviations.add(Map.of("recordedAt", String.valueOf(p.get("recorded_at")), "latitude", lat, "longitude", lng, "metersFromRoute", Math.round(dist)));
                }
            }
        }
        out.put("deviations", deviations.size() > 200 ? deviations.subList(0, 200) : deviations);
        out.put("deviationCount", deviations.size());

        // unauthorised stops
        List<double[]> allowed = new ArrayList<>();
        var jr = j.get(0);
        if (jr.get("start_lat") != null) allowed.add(new double[]{((BigDecimal) jr.get("start_lat")).doubleValue(), ((BigDecimal) jr.get("start_lng")).doubleValue()});
        if (jr.get("dest_lat") != null) allowed.add(new double[]{((BigDecimal) jr.get("dest_lat")).doubleValue(), ((BigDecimal) jr.get("dest_lng")).doubleValue()});
        for (var s : jdbc.queryForList("SELECT lat, lng FROM journey_stops WHERE company_id=? AND journey_id=? AND lat IS NOT NULL", c, journeyId))
            allowed.add(new double[]{((BigDecimal) s.get("lat")).doubleValue(), ((BigDecimal) s.get("lng")).doubleValue()});

        List<Map<String, Object>> stops = new ArrayList<>();
        int i = 0;
        while (i < pings.size()) {
            if (!slow(pings.get(i))) { i++; continue; }
            int k = i;
            while (k + 1 < pings.size() && slow(pings.get(k + 1))) k++;
            long minutes = (((java.sql.Timestamp) pings.get(k).get("recorded_at")).getTime() - ((java.sql.Timestamp) pings.get(i).get("recorded_at")).getTime()) / 60000;
            if (minutes >= minStopMinutes) {
                double lat = ((BigDecimal) pings.get(i).get("latitude")).doubleValue(), lng = ((BigDecimal) pings.get(i).get("longitude")).doubleValue();
                boolean planned = allowed.stream().anyMatch(a -> Geo.haversine(lat, lng, a[0], a[1]) <= stopRadiusM);
                if (!planned) stops.add(Map.of("from", String.valueOf(pings.get(i).get("recorded_at")), "to", String.valueOf(pings.get(k).get("recorded_at")), "minutes", minutes, "latitude", lat, "longitude", lng));
            }
            i = k + 1;
        }
        out.put("unauthorizedStops", stops);
        return out;
    }

    private static boolean slow(Map<String, Object> p) {
        Object s = p.get("speed_kmh");
        return s != null && ((BigDecimal) s).doubleValue() <= 2.0;
    }
}
