package com.fayzo.fleetpulse.domain;

import com.fayzo.fleetpulse.security.Permission;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/inventory-history")
public class InventoryHistoryController extends CrudController<InventoryHistory, InventoryHistoryRepository> {
    public InventoryHistoryController(InventoryHistoryRepository repo, CrudDeps deps) {
        super(InventoryHistory.class, repo, deps, Permission.WAREHOUSE_READ, Permission.WAREHOUSE_WRITE,
                Map.of("itemId", "inventory_items"), "InventoryHistory");
    }
}
