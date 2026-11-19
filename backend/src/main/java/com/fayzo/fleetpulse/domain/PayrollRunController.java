package com.fayzo.fleetpulse.domain;

import com.fayzo.fleetpulse.security.Permission;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payroll-runs")
public class PayrollRunController extends CrudController<PayrollRun, PayrollRunRepository> {
    public PayrollRunController(PayrollRunRepository repo, CrudDeps deps) {
        super(PayrollRun.class, repo, deps, Permission.PAYROLL_READ, Permission.PAYROLL_WRITE,
                Map.of(), "PayrollRun");
    }
}
