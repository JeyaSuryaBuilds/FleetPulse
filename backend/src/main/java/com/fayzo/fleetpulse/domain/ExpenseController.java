package com.fayzo.fleetpulse.domain;

import com.fayzo.fleetpulse.security.Permission;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/expenses")
public class ExpenseController extends CrudController<Expense, ExpenseRepository> {
    public ExpenseController(ExpenseRepository repo, CrudDeps deps) {
        super(Expense.class, repo, deps, Permission.FINANCE_READ, Permission.FINANCE_WRITE,
                Map.of("vehicleId", "vehicles", "vendorId", "vendors"), "Expense");
    }
}
