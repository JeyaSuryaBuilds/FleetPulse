package com.fayzo.fleetpulse.domain;

import com.fayzo.fleetpulse.security.Permission;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/warehouses")
public class WarehouseController extends CrudController<Warehouse, WarehouseRepository> {
    public WarehouseController(WarehouseRepository repo, CrudDeps deps) {
        super(Warehouse.class, repo, deps, Permission.WAREHOUSE_READ, Permission.WAREHOUSE_WRITE,
                Map.of("branchId", "branches"), "Warehouse");
    }
}
