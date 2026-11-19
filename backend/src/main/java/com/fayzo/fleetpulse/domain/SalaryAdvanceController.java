package com.fayzo.fleetpulse.domain;

import com.fayzo.fleetpulse.security.Permission;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/salary-advances")
public class SalaryAdvanceController extends CrudController<SalaryAdvance, SalaryAdvanceRepository> {
    public SalaryAdvanceController(SalaryAdvanceRepository repo, CrudDeps deps) {
        super(SalaryAdvance.class, repo, deps, Permission.PAYROLL_READ, Permission.PAYROLL_WRITE,
                Map.of("employeeId", "employees"), "SalaryAdvance");
    }
}
