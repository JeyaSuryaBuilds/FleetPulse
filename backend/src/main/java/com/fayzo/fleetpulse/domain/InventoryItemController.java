package com.fayzo.fleetpulse.domain;

import com.fayzo.fleetpulse.security.Permission;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/inventory-items")
public class InventoryItemController extends CrudController<InventoryItem, InventoryItemRepository> {
    public InventoryItemController(InventoryItemRepository repo, CrudDeps deps) {
        super(InventoryItem.class, repo, deps, Permission.WAREHOUSE_READ, Permission.WAREHOUSE_WRITE,
                Map.of("warehouseId", "warehouses"), "InventoryItem");
    }
}
