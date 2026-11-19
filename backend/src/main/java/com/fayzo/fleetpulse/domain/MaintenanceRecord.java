package com.fayzo.fleetpulse.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "maintenance_records")
public class MaintenanceRecord extends BaseEntity {
    @NotNull
    @Column(name = "vehicle_id")
    private Long vehicleId;

    @NotBlank @Size(max = 30)
    @Column(name = "kind")
    private String kind;

    @NotBlank @Size(max = 160)
    @Column(name = "title")
    private String title;

    
    @Column(name = "description")
    private String description;

    @NotBlank @Size(max = 20)
    @Column(name = "status")
    private String status = "OPEN";

    
    @Column(name = "scheduled_on")
    private LocalDate scheduledOn;

    
    @Column(name = "completed_on")
    private LocalDate completedOn;

    @Digits(integer = 11, fraction = 1)
    @Column(name = "odometer_km")
    private BigDecimal odometerKm;

    
    @Column(name = "service_centre_id")
    private Long serviceCentreId;

    @Digits(integer = 10, fraction = 2)
    @Column(name = "cost")
    private BigDecimal cost;

    
    @Column(name = "next_due_date")
    private LocalDate nextDueDate;

    @Digits(integer = 11, fraction = 1)
    @Column(name = "next_due_km")
    private BigDecimal nextDueKm;
}
