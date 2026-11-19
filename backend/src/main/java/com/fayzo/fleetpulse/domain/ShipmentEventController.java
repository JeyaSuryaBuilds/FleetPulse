package com.fayzo.fleetpulse.domain;

import com.fayzo.fleetpulse.security.Permission;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/shipment-events")
public class ShipmentEventController extends CrudController<ShipmentEvent, ShipmentEventRepository> {
    public ShipmentEventController(ShipmentEventRepository repo, CrudDeps deps) {
        super(ShipmentEvent.class, repo, deps, Permission.SHIPMENT_READ, Permission.SHIPMENT_WRITE,
                Map.of("shipmentId", "shipments"), "ShipmentEvent");
    }
}
