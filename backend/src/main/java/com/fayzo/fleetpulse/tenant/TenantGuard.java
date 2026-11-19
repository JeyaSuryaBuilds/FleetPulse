package com.fayzo.fleetpulse.tenant;

import com.fayzo.fleetpulse.common.ApiException;

import java.util.Optional;

public final class TenantGuard {
    private TenantGuard() {}
    /** Returns the entity only if it belongs to the current tenant; otherwise behaves as not found (no existence leak). */
    public static <T extends TenantAwareEntity> T owned(Optional<T> found, String what) {
        T e = found.orElseThrow(() -> ApiException.notFound(what + " not found"));
        if (!e.getCompanyId().equals(TenantContext.require())) throw ApiException.notFound(what + " not found");
        return e;
    }
}
