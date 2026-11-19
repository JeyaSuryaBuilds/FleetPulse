package com.fayzo.fleetpulse.domain;

import com.fayzo.fleetpulse.security.Permission;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/attendance")
public class AttendanceController extends CrudController<Attendance, AttendanceRepository> {
    public AttendanceController(AttendanceRepository repo, CrudDeps deps) {
        super(Attendance.class, repo, deps, Permission.EMPLOYEE_READ, Permission.EMPLOYEE_WRITE,
                Map.of("employeeId", "employees"), "Attendance");
    }
}
