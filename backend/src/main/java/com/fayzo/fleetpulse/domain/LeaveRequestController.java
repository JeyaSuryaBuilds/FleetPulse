package com.fayzo.fleetpulse.domain;

import com.fayzo.fleetpulse.security.Permission;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/leave-requests")
public class LeaveRequestController extends CrudController<LeaveRequest, LeaveRequestRepository> {
    public LeaveRequestController(LeaveRequestRepository repo, CrudDeps deps) {
        super(LeaveRequest.class, repo, deps, Permission.EMPLOYEE_READ, Permission.EMPLOYEE_WRITE,
                Map.of("employeeId", "employees"), "LeaveRequest");
    }
}
