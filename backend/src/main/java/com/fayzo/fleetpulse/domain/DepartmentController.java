package com.fayzo.fleetpulse.domain;

import com.fayzo.fleetpulse.security.Permission;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/departments")
public class DepartmentController extends CrudController<Department, DepartmentRepository> {
    public DepartmentController(DepartmentRepository repo, CrudDeps deps) {
        super(Department.class, repo, deps, Permission.DASHBOARD_VIEW, Permission.ORG_MANAGE,
                Map.of("branchId", "branches"), "Department");
    }
}
