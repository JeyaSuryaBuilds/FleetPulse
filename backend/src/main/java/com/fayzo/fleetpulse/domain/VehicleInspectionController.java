package com.fayzo.fleetpulse.domain;

import com.fayzo.fleetpulse.security.Permission;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/vehicle-inspections")
public class VehicleInspectionController extends CrudController<VehicleInspection, VehicleInspectionRepository> {
    public VehicleInspectionController(VehicleInspectionRepository repo, CrudDeps deps) {
        super(VehicleInspection.class, repo, deps, Permission.FLEET_READ, Permission.FLEET_WRITE,
                Map.of("vehicleId", "vehicles"), "VehicleInspection");
    }
}
