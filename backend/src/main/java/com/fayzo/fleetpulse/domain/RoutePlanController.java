package com.fayzo.fleetpulse.domain;

import com.fayzo.fleetpulse.security.Permission;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/routes")
public class RoutePlanController extends CrudController<RoutePlan, RoutePlanRepository> {
    public RoutePlanController(RoutePlanRepository repo, CrudDeps deps) {
        super(RoutePlan.class, repo, deps, Permission.ROUTE_READ, Permission.ROUTE_WRITE,
                Map.of("journeyId", "journeys"), "RoutePlan");
    }
}
