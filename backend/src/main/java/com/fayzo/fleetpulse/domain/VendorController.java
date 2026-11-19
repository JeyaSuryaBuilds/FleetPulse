package com.fayzo.fleetpulse.domain;

import com.fayzo.fleetpulse.security.Permission;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/vendors")
public class VendorController extends CrudController<Vendor, VendorRepository> {
    public VendorController(VendorRepository repo, CrudDeps deps) {
        super(Vendor.class, repo, deps, Permission.VENDOR_READ, Permission.VENDOR_WRITE,
                Map.of(), "Vendor");
    }
}
