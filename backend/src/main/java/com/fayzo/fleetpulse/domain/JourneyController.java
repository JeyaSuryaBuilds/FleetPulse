package com.fayzo.fleetpulse.domain;

import com.fayzo.fleetpulse.security.Permission;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/journeys")
public class JourneyController extends CrudController<Journey, JourneyRepository> {
    public JourneyController(JourneyRepository repo, CrudDeps deps) {
        super(Journey.class, repo, deps, Permission.JOURNEY_READ, Permission.JOURNEY_WRITE,
                Map.of("vehicleId", "vehicles", "driverId", "drivers"), "Journey");
    }
}
