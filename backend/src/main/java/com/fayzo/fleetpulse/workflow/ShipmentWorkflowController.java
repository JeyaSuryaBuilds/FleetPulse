package com.fayzo.fleetpulse.workflow;

import com.fayzo.fleetpulse.common.Authz;
import com.fayzo.fleetpulse.security.Permission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/shipments")
public class ShipmentWorkflowController {
    public record Body(@NotBlank String status, @Size(max = 300) String note, BigDecimal lat, BigDecimal lng,
                       @Size(max = 300) String failureReason, @Size(max = 500) String podUrl, @Size(max = 120) String podSignedBy) {}
    public record Assign(@jakarta.validation.constraints.NotNull Long driverId, Long vehicleId) {}
    private final ShipmentWorkflowService service;
    public ShipmentWorkflowController(ShipmentWorkflowService service) { this.service = service; }

    /** Staff status change with validated transitions; also writes the customer-visible timeline entry. */
    @PostMapping("/{id}/status")
    public Map<String, Object> change(@PathVariable long id, @Valid @RequestBody Body b) {
        Authz.require(Permission.SHIPMENT_WRITE);
        return service.change(id, new ShipmentWorkflowService.StatusChange(b.status(), b.note(), b.lat(), b.lng(), b.failureReason(), b.podUrl(), b.podSignedBy()), null);
    }

    @PostMapping("/{id}/assign")
    public Map<String, Object> assign(@PathVariable long id, @Valid @RequestBody Assign a) {
        Authz.require(Permission.SHIPMENT_WRITE);
        return service.assign(id, a.driverId(), a.vehicleId());
    }
}
