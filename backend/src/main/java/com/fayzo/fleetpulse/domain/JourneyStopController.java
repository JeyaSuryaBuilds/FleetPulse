package com.fayzo.fleetpulse.domain;

import com.fayzo.fleetpulse.security.Permission;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/journey-stops")
public class JourneyStopController extends CrudController<JourneyStop, JourneyStopRepository> {
    public JourneyStopController(JourneyStopRepository repo, CrudDeps deps) {
        super(JourneyStop.class, repo, deps, Permission.JOURNEY_READ, Permission.JOURNEY_WRITE,
                Map.of("journeyId", "journeys"), "JourneyStop");
    }
}
