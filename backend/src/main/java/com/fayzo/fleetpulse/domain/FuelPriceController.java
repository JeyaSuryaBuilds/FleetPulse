package com.fayzo.fleetpulse.domain;

import com.fayzo.fleetpulse.security.Permission;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/fuel-prices")
public class FuelPriceController extends CrudController<FuelPrice, FuelPriceRepository> {
    public FuelPriceController(FuelPriceRepository repo, CrudDeps deps) {
        super(FuelPrice.class, repo, deps, Permission.FUEL_READ, Permission.FUEL_WRITE,
                Map.of(), "FuelPrice");
    }
}
