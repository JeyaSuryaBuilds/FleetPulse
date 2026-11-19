package com.fayzo.fleetpulse.domain;

import com.fayzo.fleetpulse.security.Permission;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/salary-structures")
public class SalaryStructureController extends CrudController<SalaryStructure, SalaryStructureRepository> {
    public SalaryStructureController(SalaryStructureRepository repo, CrudDeps deps) {
        super(SalaryStructure.class, repo, deps, Permission.PAYROLL_READ, Permission.PAYROLL_WRITE,
                Map.of("employeeId", "employees"), "SalaryStructure");
    }
}
