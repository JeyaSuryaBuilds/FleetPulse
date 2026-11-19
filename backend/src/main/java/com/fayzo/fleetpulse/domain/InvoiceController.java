package com.fayzo.fleetpulse.domain;

import com.fayzo.fleetpulse.security.Permission;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/invoices")
public class InvoiceController extends CrudController<Invoice, InvoiceRepository> {
    public InvoiceController(InvoiceRepository repo, CrudDeps deps) {
        super(Invoice.class, repo, deps, Permission.FINANCE_READ, Permission.FINANCE_WRITE,
                Map.of("customerId", "customers", "vendorId", "vendors", "shipmentId", "shipments"), "Invoice");
    }
}
