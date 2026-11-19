package com.fayzo.fleetpulse.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "vehicle_categories")
public class VehicleCategory extends BaseEntity {
    @NotBlank @Size(max = 80)
    @Column(name = "name")
    private String name;

    @Size(max = 300)
    @Column(name = "description")
    private String description;

    @Size(max = 20)
    @Column(name = "default_fuel_type")
    private String defaultFuelType;
}
