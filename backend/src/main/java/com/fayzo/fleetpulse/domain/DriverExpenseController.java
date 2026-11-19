package com.fayzo.fleetpulse.domain;

import com.fayzo.fleetpulse.security.Permission;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/driver-expenses")
public class DriverExpenseController extends CrudController<DriverExpense, DriverExpenseRepository> {
    public DriverExpenseController(DriverExpenseRepository repo, CrudDeps deps) {
        super(DriverExpense.class, repo, deps, Permission.DRIVER_READ, Permission.DRIVER_WRITE,
                Map.of("driverId", "drivers", "journeyId", "journeys"), "DriverExpense");
    }
}
