package com.fayzo.fleetpulse.domain;

import com.fayzo.fleetpulse.security.Permission;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payslips")
public class PayslipController extends CrudController<Payslip, PayslipRepository> {
    public PayslipController(PayslipRepository repo, CrudDeps deps) {
        super(Payslip.class, repo, deps, Permission.PAYROLL_READ, Permission.PAYROLL_WRITE,
                Map.of("payrollRunId", "payroll_runs", "employeeId", "employees"), "Payslip");
    }
}
