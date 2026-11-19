package com.fayzo.fleetpulse.domain;

import com.fayzo.fleetpulse.security.Permission;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController extends CrudController<Customer, CustomerRepository> {
    public CustomerController(CustomerRepository repo, CrudDeps deps) {
        super(Customer.class, repo, deps, Permission.CUSTOMER_READ, Permission.CUSTOMER_WRITE,
                Map.of(), "Customer");
    }
}
