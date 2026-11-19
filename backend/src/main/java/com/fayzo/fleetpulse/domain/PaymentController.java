package com.fayzo.fleetpulse.domain;

import com.fayzo.fleetpulse.security.Permission;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController extends CrudController<Payment, PaymentRepository> {
    public PaymentController(PaymentRepository repo, CrudDeps deps) {
        super(Payment.class, repo, deps, Permission.FINANCE_READ, Permission.FINANCE_WRITE,
                Map.of("invoiceId", "invoices"), "Payment");
    }
}
