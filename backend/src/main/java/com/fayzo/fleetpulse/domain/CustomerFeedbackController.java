package com.fayzo.fleetpulse.domain;

import com.fayzo.fleetpulse.security.Permission;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/customer-feedback")
public class CustomerFeedbackController extends CrudController<CustomerFeedback, CustomerFeedbackRepository> {
    public CustomerFeedbackController(CustomerFeedbackRepository repo, CrudDeps deps) {
        super(CustomerFeedback.class, repo, deps, Permission.CUSTOMER_READ, Permission.CUSTOMER_WRITE,
                Map.of("customerId", "customers", "shipmentId", "shipments"), "CustomerFeedback");
    }
}
