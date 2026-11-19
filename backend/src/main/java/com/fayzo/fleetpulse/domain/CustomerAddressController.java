package com.fayzo.fleetpulse.domain;

import com.fayzo.fleetpulse.security.Permission;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/customer-addresses")
public class CustomerAddressController extends CrudController<CustomerAddress, CustomerAddressRepository> {
    public CustomerAddressController(CustomerAddressRepository repo, CrudDeps deps) {
        super(CustomerAddress.class, repo, deps, Permission.CUSTOMER_READ, Permission.CUSTOMER_WRITE,
                Map.of("customerId", "customers"), "CustomerAddress");
    }
}
