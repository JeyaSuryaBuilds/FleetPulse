package com.fayzo.fleetpulse.routing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fayzo.fleetpulse.common.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.*;

/**
 * OpenRouteService client (OpenStreetMap data). Works with the hosted API (FP_ORS_API_KEY) or a self-hosted ORS (FP_ORS_BASE_URL).
 * Without an API key every call fails with ROUTING_NOT_CONFIGURED; no route is ever invented.
 */
@Component
public class RoutingClient {
    public record Point(double lat, double lng) {}
    public record Route(double distanceMeters, double durationSeconds, List<double[]> coordinates, double tollRoadMeters) {}

    private final RestClient http;
    private final String apiKey;

    public RoutingClient(@Value("${fleetpulse.routing.base-url}") String baseUrl, @Value("${fleetpulse.routing.api-key:}") String apiKey) {
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(Duration.ofSeconds(10));
        rf.setReadTimeout(Duration.ofSeconds(30));
        this.http = RestClient.builder().requestFactory(rf).baseUrl(baseUrl).build();
        this.apiKey = apiKey;
    }

    private void requireKey() {
        if (apiKey == null || apiKey.isBlank()) throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "ROUTING_NOT_CONFIGURED", "Routing is not configured on this server (missing FP_ORS_API_KEY)");
    }

    /** preference: fastest | shortest | recommended */
    public Route directions(List<Point> points, String preference) {
        requireKey();
        List<List<Double>> coords = new ArrayList<>();
        for (Point p : points) coords.add(List.of(p.lng(), p.lat()));
        try {
            JsonNode r = http.post().uri("/v2/directions/driving-car/geojson").header("Authorization", apiKey)
                    .body(Map.of("coordinates", coords, "preference", preference, "extra_info", List.of("tollways"), "instructions", false))
                    .retrieve().body(JsonNode.class);
            JsonNode f = r.path("features").path(0);
            JsonNode sum = f.path("properties").path("summary");
            List<double[]> line = new ArrayList<>();
            for (JsonNode c : f.path("geometry").path("coordinates")) line.add(new double[]{c.get(1).asDouble(), c.get(0).asDouble()});
            double toll = 0;
            JsonNode tv = f.path("properties").path("extras").path("tollways").path("values");
            for (JsonNode seg : tv) if (seg.get(2).asInt() == 1) toll += segmentLength(line, seg.get(0).asInt(), seg.get(1).asInt());
            if (line.isEmpty()) throw new ApiException(HttpStatus.BAD_GATEWAY, "ROUTING_EMPTY", "No route found between these points");
            return new Route(sum.path("distance").asDouble(), sum.path("duration").asDouble(), line, toll);
        } catch (ApiException e) { throw e; }
        catch (Exception e) { throw new ApiException(HttpStatus.BAD_GATEWAY, "ROUTING_UPSTREAM", "The routing service could not compute this route"); }
    }

    /** Duration matrix in seconds between all points (row = from, col = to). */
    public double[][] durationMatrix(List<Point> points) {
        requireKey();
        List<List<Double>> locs = new ArrayList<>();
        for (Point p : points) locs.add(List.of(p.lng(), p.lat()));
        try {
            JsonNode r = http.post().uri("/v2/matrix/driving-car").header("Authorization", apiKey)
                    .body(Map.of("locations", locs, "metrics", List.of("duration"))).retrieve().body(JsonNode.class);
            int n = points.size();
            double[][] m = new double[n][n];
            for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) {
                JsonNode v = r.path("durations").path(i).path(j);
                if (v.isNull() || v.isMissingNode()) throw new ApiException(HttpStatus.BAD_REQUEST, "UNROUTABLE", "A location cannot be reached by road");
                m[i][j] = v.asDouble();
            }
            return m;
        } catch (ApiException e) { throw e; }
        catch (Exception e) { throw new ApiException(HttpStatus.BAD_GATEWAY, "ROUTING_UPSTREAM", "The routing service could not compute the matrix"); }
    }

    private static double segmentLength(List<double[]> line, int from, int to) {
        double d = 0;
        for (int i = Math.max(from, 0); i < Math.min(to, line.size() - 1); i++) d += Geo.haversine(line.get(i)[0], line.get(i)[1], line.get(i + 1)[0], line.get(i + 1)[1]);
        return d;
    }
}
