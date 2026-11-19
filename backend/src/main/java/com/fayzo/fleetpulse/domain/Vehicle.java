package com.fayzo.fleetpulse.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "vehicles")
public class Vehicle extends BaseEntity {
    @NotBlank @Size(max = 30)
    @Column(name = "registration_number")
    private String registrationNumber;

    
    @Column(name = "category_id")
    private Long categoryId;

    @Size(max = 60)
    @Column(name = "make")
    private String make;

    @Size(max = 60)
    @Column(name = "model")
    private String model;

    
    @Column(name = "manufacture_year")
    private Integer manufactureYear;

    @Size(max = 40)
    @Column(name = "vin")
    private String vin;

    @Size(max = 20)
    @Column(name = "fuel_type")
    private String fuelType;

    @Digits(integer = 6, fraction = 2)
    @Column(name = "tank_capacity_litres")
    private BigDecimal tankCapacityLitres;

    @Digits(integer = 8, fraction = 2)
    @Column(name = "load_capacity_kg")
    private BigDecimal loadCapacityKg;

    @NotBlank @Size(max = 20)
    @Column(name = "status")
    private String status = "AVAILABLE";

    @Digits(integer = 11, fraction = 1)
    @Column(name = "odometer_km")
    private BigDecimal odometerKm;

    
    @Column(name = "branch_id")
    private Long branchId;

    
    @Column(name = "assigned_driver_id")
    private Long assignedDriverId;

    @Size(max = 60)
    @Column(name = "gps_device_id")
    private String gpsDeviceId;

    
    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    @Digits(integer = 12, fraction = 2)
    @Column(name = "purchase_price")
    private BigDecimal purchasePrice;

    @Digits(integer = 12, fraction = 2)
    @Column(name = "residual_value")
    private BigDecimal residualValue;

    
    @Column(name = "useful_life_years")
    private Integer usefulLifeYears;

    
    @Column(name = "sale_date")
    private LocalDate saleDate;

    @Digits(integer = 12, fraction = 2)
    @Column(name = "sale_price")
    private BigDecimal salePrice;
}
