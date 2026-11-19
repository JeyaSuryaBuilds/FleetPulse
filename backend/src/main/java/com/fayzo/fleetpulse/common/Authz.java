package com.fayzo.fleetpulse.common;

import com.fayzo.fleetpulse.security.AuthPrincipal;
import com.fayzo.fleetpulse.security.Permission;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class Authz {
    private Authz() {}

    public static AuthPrincipal principal() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !(a.getPrincipal() instanceof AuthPrincipal p)) throw ApiException.unauthorized("Authentication required");
        return p;
    }

    public static void require(Permission perm) {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || a.getAuthorities().stream().noneMatch(g -> g.getAuthority().equals(perm.name())))
            throw ApiException.forbidden("Missing permission " + perm.name());
    }

    public static boolean has(Permission perm) {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a != null && a.getAuthorities().stream().anyMatch(g -> g.getAuthority().equals(perm.name()));
    }
}
