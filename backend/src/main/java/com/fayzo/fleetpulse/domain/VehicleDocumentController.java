package com.fayzo.fleetpulse.domain;

import com.fayzo.fleetpulse.security.Permission;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/vehicle-documents")
public class VehicleDocumentController extends CrudController<VehicleDocument, VehicleDocumentRepository> {
    public VehicleDocumentController(VehicleDocumentRepository repo, CrudDeps deps) {
        super(VehicleDocument.class, repo, deps, Permission.FLEET_READ, Permission.FLEET_WRITE,
                Map.of("vehicleId", "vehicles"), "VehicleDocument");
    }
}
