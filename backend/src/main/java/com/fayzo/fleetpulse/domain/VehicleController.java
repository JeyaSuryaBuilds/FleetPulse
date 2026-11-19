package com.fayzo.fleetpulse.domain;

import com.fayzo.fleetpulse.security.Permission;
import com.fayzo.fleetpulse.subscription.PlanLimits;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/vehicles")
public class VehicleController extends CrudController<Vehicle, VehicleRepository> {
    private final PlanLimits limits;

    public VehicleController(VehicleRepository repo, CrudDeps deps, PlanLimits limits) {
        super(Vehicle.class, repo, deps, Permission.FLEET_READ, Permission.FLEET_WRITE,
                Map.of("categoryId", "vehicle_categories", "branchId", "branches", "assignedDriverId", "drivers"), "Vehicle");
        this.limits = limits;
    }

    @Override
    protected void beforeCreate(Vehicle v, com.fasterxml.jackson.databind.JsonNode body) { limits.requireVehicleCapacity(); }
}
