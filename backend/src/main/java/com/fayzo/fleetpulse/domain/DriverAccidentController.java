package com.fayzo.fleetpulse.domain;

import com.fayzo.fleetpulse.security.Permission;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/driver-accidents")
public class DriverAccidentController extends CrudController<DriverAccident, DriverAccidentRepository> {
    public DriverAccidentController(DriverAccidentRepository repo, CrudDeps deps) {
        super(DriverAccident.class, repo, deps, Permission.DRIVER_READ, Permission.DRIVER_WRITE,
                Map.of("driverId", "drivers", "vehicleId", "vehicles"), "DriverAccident");
    }
}
