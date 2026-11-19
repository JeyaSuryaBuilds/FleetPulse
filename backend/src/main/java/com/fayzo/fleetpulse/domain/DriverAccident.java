package com.fayzo.fleetpulse.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "driver_accidents")
public class DriverAccident extends BaseEntity {
    @NotNull
    @Column(name = "driver_id")
    private Long driverId;

    
    @Column(name = "vehicle_id")
    private Long vehicleId;

    @NotNull
    @Column(name = "occurred_on")
    private LocalDate occurredOn;

    @Size(max = 300)
    @Column(name = "location")
    private String location;

    @Size(max = 20)
    @Column(name = "severity")
    private String severity;

    @Digits(integer = 10, fraction = 2)
    @Column(name = "cost_estimate")
    private BigDecimal costEstimate;

    
    @Column(name = "description")
    private String description;
}
