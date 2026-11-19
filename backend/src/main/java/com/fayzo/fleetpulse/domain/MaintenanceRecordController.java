package com.fayzo.fleetpulse.domain;

import com.fayzo.fleetpulse.security.Permission;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/maintenance")
public class MaintenanceRecordController extends CrudController<MaintenanceRecord, MaintenanceRecordRepository> {
    public MaintenanceRecordController(MaintenanceRecordRepository repo, CrudDeps deps) {
        super(MaintenanceRecord.class, repo, deps, Permission.MAINTENANCE_READ, Permission.MAINTENANCE_WRITE,
                Map.of("vehicleId", "vehicles", "serviceCentreId", "vendors"), "MaintenanceRecord");
    }
}
