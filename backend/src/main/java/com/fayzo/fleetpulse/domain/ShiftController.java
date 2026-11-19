package com.fayzo.fleetpulse.domain;

import com.fayzo.fleetpulse.security.Permission;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/shifts")
public class ShiftController extends CrudController<Shift, ShiftRepository> {
    public ShiftController(ShiftRepository repo, CrudDeps deps) {
        super(Shift.class, repo, deps, Permission.EMPLOYEE_READ, Permission.EMPLOYEE_WRITE,
                Map.of(), "Shift");
    }
}
