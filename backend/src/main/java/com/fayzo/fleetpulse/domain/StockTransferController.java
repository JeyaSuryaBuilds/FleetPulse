package com.fayzo.fleetpulse.domain;

import com.fayzo.fleetpulse.security.Permission;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/stock-transfers")
public class StockTransferController extends CrudController<StockTransfer, StockTransferRepository> {
    public StockTransferController(StockTransferRepository repo, CrudDeps deps) {
        super(StockTransfer.class, repo, deps, Permission.WAREHOUSE_READ, Permission.WAREHOUSE_WRITE,
                Map.of("fromWarehouseId", "warehouses", "toWarehouseId", "warehouses", "itemId", "inventory_items"), "StockTransfer");
    }
}
