package com.fayzo.fleetpulse.domain;

import com.fayzo.fleetpulse.security.Permission;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/drivers")
public class DriverController extends CrudController<Driver, DriverRepository> {
    public DriverController(DriverRepository repo, CrudDeps deps) {
        super(Driver.class, repo, deps, Permission.DRIVER_READ, Permission.DRIVER_WRITE,
                Map.of("userId", "users", "branchId", "branches"), "Driver");
    }
}
