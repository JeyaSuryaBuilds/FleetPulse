package com.fayzo.fleetpulse.domain;

import com.fayzo.fleetpulse.security.Permission;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/vehicle-categories")
public class VehicleCategoryController extends CrudController<VehicleCategory, VehicleCategoryRepository> {
    public VehicleCategoryController(VehicleCategoryRepository repo, CrudDeps deps) {
        super(VehicleCategory.class, repo, deps, Permission.FLEET_READ, Permission.FLEET_WRITE,
                Map.of(), "VehicleCategory");
    }
}
