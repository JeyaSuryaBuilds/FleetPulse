package com.fayzo.fleetpulse.domain;

import com.fayzo.fleetpulse.security.Permission;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/tasks")
public class WorkTaskController extends CrudController<WorkTask, WorkTaskRepository> {
    public WorkTaskController(WorkTaskRepository repo, CrudDeps deps) {
        super(WorkTask.class, repo, deps, Permission.EMPLOYEE_READ, Permission.EMPLOYEE_WRITE,
                Map.of("assignedEmployeeId", "employees"), "WorkTask");
    }
}
