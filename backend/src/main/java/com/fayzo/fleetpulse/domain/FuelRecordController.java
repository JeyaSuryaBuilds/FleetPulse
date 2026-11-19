package com.fayzo.fleetpulse.domain;

import com.fayzo.fleetpulse.security.Permission;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/fuel-records")
public class FuelRecordController extends CrudController<FuelRecord, FuelRecordRepository> {
    public FuelRecordController(FuelRecordRepository repo, CrudDeps deps) {
        super(FuelRecord.class, repo, deps, Permission.FUEL_READ, Permission.FUEL_WRITE,
                Map.of("vehicleId", "vehicles", "driverId", "drivers", "journeyId", "journeys", "vendorId", "vendors"), "FuelRecord");
    }
}
