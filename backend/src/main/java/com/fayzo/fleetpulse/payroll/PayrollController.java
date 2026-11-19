package com.fayzo.fleetpulse.payroll;

import com.fayzo.fleetpulse.common.Authz;
import com.fayzo.fleetpulse.security.Permission;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payroll")
public class PayrollController {
    private final PayrollService service;
    public PayrollController(PayrollService service) { this.service = service; }

    @PostMapping("/generate/{periodMonth}")
    public Map<String, Object> generate(@PathVariable String periodMonth) { Authz.require(Permission.PAYROLL_WRITE); return service.generate(periodMonth); }

    @PostMapping("/runs/{id}/finalize")
    public Map<String, Object> finalizeRun(@PathVariable long id) { Authz.require(Permission.PAYROLL_WRITE); return service.finalizeRun(id); }

    @PostMapping("/runs/{id}/mark-paid")
    public Map<String, Object> markPaid(@PathVariable long id) { Authz.require(Permission.PAYROLL_WRITE); return service.markPaid(id); }

    /** Manager approval of verified completed work (per delivery / trip / distance / task) before it can be paid. */
    @PostMapping("/work-records/{id}/approve")
    public Map<String, Boolean> approve(@PathVariable long id) { Authz.require(Permission.PAYROLL_WRITE); service.decideWork(id, true, Authz.principal().subjectId()); return Map.of("ok", true); }

    @PostMapping("/work-records/{id}/reject")
    public Map<String, Boolean> reject(@PathVariable long id) { Authz.require(Permission.PAYROLL_WRITE); service.decideWork(id, false, Authz.principal().subjectId()); return Map.of("ok", true); }
}
