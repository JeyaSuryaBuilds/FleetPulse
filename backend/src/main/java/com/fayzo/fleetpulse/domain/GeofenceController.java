package com.fayzo.fleetpulse.domain;

import com.fayzo.fleetpulse.security.Permission;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/geofences")
public class GeofenceController extends CrudController<Geofence, GeofenceRepository> {
    public GeofenceController(GeofenceRepository repo, CrudDeps deps) {
        super(Geofence.class, repo, deps, Permission.GPS_READ, Permission.ROUTE_WRITE,
                Map.of(), "Geofence");
    }
}
