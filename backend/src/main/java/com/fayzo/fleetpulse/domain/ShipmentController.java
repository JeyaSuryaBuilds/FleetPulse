package com.fayzo.fleetpulse.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fayzo.fleetpulse.common.ApiException;
import com.fayzo.fleetpulse.security.Permission;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/shipments")
public class ShipmentController extends CrudController<Shipment, ShipmentRepository> {
    public ShipmentController(ShipmentRepository repo, CrudDeps deps) {
        super(Shipment.class, repo, deps, Permission.SHIPMENT_READ, Permission.SHIPMENT_WRITE,
                Map.of("customerId", "customers", "journeyId", "journeys", "vehicleId", "vehicles", "driverId", "drivers", "warehouseId", "warehouses"), "Shipment");
    }

    @Override
    protected void beforeCreate(Shipment s, JsonNode body) {
        String st = body.path("status").asText("CREATED");
        if (!st.equals("CREATED") && !st.equals("ASSIGNED")) throw ApiException.badRequest("BAD_STATUS", "New shipments start as CREATED or ASSIGNED");
        if (body.has("deliveredAt") || body.has("podUrl") || body.has("podSignedBy"))
            throw ApiException.badRequest("USE_WORKFLOW", "Delivery fields are set through POST /shipments/{id}/status");
    }

    @Override
    protected void beforeUpdate(Shipment existing, JsonNode body) {
        for (String f : new String[]{"status", "deliveredAt", "podUrl", "podSignedBy", "failureReason", "trackingId"})
            if (body.has(f)) throw ApiException.badRequest("USE_WORKFLOW", f + " cannot be edited here. Use POST /api/v1/shipments/{id}/status");
    }
}
