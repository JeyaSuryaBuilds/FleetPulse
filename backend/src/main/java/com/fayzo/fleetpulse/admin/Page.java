package com.fayzo.fleetpulse.admin;

import java.util.List;
import java.util.Map;

public final class Page {
    private Page() {}
    public static int size(int s) { return Math.min(Math.max(s, 1), 100); }
    public static Map<String, Object> of(List<?> content, int page, int size, long total) {
        return Map.of("content", content, "page", page, "size", size, "total", total);
    }
}
