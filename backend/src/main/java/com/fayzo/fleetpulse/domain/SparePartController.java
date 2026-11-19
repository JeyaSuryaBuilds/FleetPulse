package com.fayzo.fleetpulse.domain;

import com.fayzo.fleetpulse.security.Permission;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/spare-parts")
public class SparePartController extends CrudController<SparePart, SparePartRepository> {
    public SparePartController(SparePartRepository repo, CrudDeps deps) {
        super(SparePart.class, repo, deps, Permission.MAINTENANCE_READ, Permission.MAINTENANCE_WRITE,
                Map.of("vendorId", "vendors"), "SparePart");
    }
}
