package com.fayzo.fleetpulse.domain;

import com.fayzo.fleetpulse.security.Permission;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/employees")
public class EmployeeController extends CrudController<Employee, EmployeeRepository> {
    public EmployeeController(EmployeeRepository repo, CrudDeps deps) {
        super(Employee.class, repo, deps, Permission.EMPLOYEE_READ, Permission.EMPLOYEE_WRITE,
                Map.of("userId", "users", "driverId", "drivers", "departmentId", "departments", "branchId", "branches", "shiftId", "shifts"), "Employee");
    }
}
