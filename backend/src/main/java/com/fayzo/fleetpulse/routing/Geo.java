package com.fayzo.fleetpulse.routing;

import java.util.List;

public final class Geo {
    private Geo() {}
    public static double haversine(double lat1, double lng1, double lat2, double lng2) {
        double R = 6371000, dLat = Math.toRadians(lat2 - lat1), dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * R * Math.asin(Math.min(1, Math.sqrt(a)));
    }

    /** Distance in metres from a point to a polyline (local equirectangular projection, accurate for route-scale distances). */
    public static double distanceToPolyline(double lat, double lng, List<double[]> line) {
        if (line.isEmpty()) return Double.MAX_VALUE;
        if (line.size() == 1) return haversine(lat, lng, line.get(0)[0], line.get(0)[1]);
        double best = Double.MAX_VALUE, k = Math.cos(Math.toRadians(lat)) * 111320.0;
        for (int i = 0; i + 1 < line.size(); i++) {
            double ax = (line.get(i)[1] - lng) * k, ay = (line.get(i)[0] - lat) * 110540.0;
            double bx = (line.get(i + 1)[1] - lng) * k, by = (line.get(i + 1)[0] - lat) * 110540.0;
            double dx = bx - ax, dy = by - ay, len2 = dx * dx + dy * dy;
            double t = len2 == 0 ? 0 : Math.max(0, Math.min(1, -(ax * dx + ay * dy) / len2));
            best = Math.min(best, Math.hypot(ax + t * dx, ay + t * dy));
        }
        return best;
    }
}
