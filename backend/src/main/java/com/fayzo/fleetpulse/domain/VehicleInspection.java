package com.fayzo.fleetpulse.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "vehicle_inspections")
public class VehicleInspection extends BaseEntity {
    @NotNull
    @Column(name = "vehicle_id")
    private Long vehicleId;

    @NotNull
    @Column(name = "inspected_on")
    private LocalDate inspectedOn;

    @Size(max = 120)
    @Column(name = "inspector_name")
    private String inspectorName;

    @Size(max = 20)
    @Column(name = "result")
    private String result;

    @Digits(integer = 11, fraction = 1)
    @Column(name = "odometer_km")
    private BigDecimal odometerKm;

    
    @Column(name = "notes")
    private String notes;
}
