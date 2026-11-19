package com.fayzo.fleetpulse.domain;

import com.fayzo.fleetpulse.security.Permission;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/work-records")
public class WorkRecordController extends CrudController<WorkRecord, WorkRecordRepository> {
    public WorkRecordController(WorkRecordRepository repo, CrudDeps deps) {
        super(WorkRecord.class, repo, deps, Permission.PAYROLL_READ, Permission.PAYROLL_WRITE,
                Map.of("employeeId", "employees"), "WorkRecord");
    }
}
