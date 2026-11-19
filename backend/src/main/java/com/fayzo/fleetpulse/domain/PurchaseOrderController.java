package com.fayzo.fleetpulse.domain;

import com.fayzo.fleetpulse.security.Permission;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/purchase-orders")
public class PurchaseOrderController extends CrudController<PurchaseOrder, PurchaseOrderRepository> {
    public PurchaseOrderController(PurchaseOrderRepository repo, CrudDeps deps) {
        super(PurchaseOrder.class, repo, deps, Permission.VENDOR_READ, Permission.VENDOR_WRITE,
                Map.of("vendorId", "vendors"), "PurchaseOrder");
    }
}
