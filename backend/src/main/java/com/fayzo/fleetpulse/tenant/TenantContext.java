package com.fayzo.fleetpulse.tenant;

import com.fayzo.fleetpulse.common.ApiException;

/** Holds the authenticated company for the current request thread. Cleared by JwtAuthFilter. */
public final class TenantContext {
    private static final ThreadLocal<Long> CURRENT = new ThreadLocal<>();
    private TenantContext() {}
    public static void set(Long companyId) { CURRENT.set(companyId); }
    public static Long get() { return CURRENT.get(); }
    public static long require() {
        Long id = CURRENT.get();
        if (id == null) throw ApiException.forbidden("No tenant context");
        return id;
    }
    public static void clear() { CURRENT.remove(); }
}
