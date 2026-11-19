package com.fayzo.fleetpulse.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "journeys")
public class Journey extends BaseEntity {
    @NotBlank @Size(max = 30)
    @Column(name = "journey_code")
    private String journeyCode = com.fayzo.fleetpulse.common.IdGen.code("JR");

    
    @Column(name = "vehicle_id")
    private Long vehicleId;

    
    @Column(name = "driver_id")
    private Long driverId;

    @NotBlank @Size(max = 300)
    @Column(name = "start_location")
    private String startLocation;

    @Digits(integer = 3, fraction = 7)
    @Column(name = "start_lat")
    private BigDecimal startLat;

    @Digits(integer = 3, fraction = 7)
    @Column(name = "start_lng")
    private BigDecimal startLng;

    @NotBlank @Size(max = 300)
    @Column(name = "destination")
    private String destination;

    @Digits(integer = 3, fraction = 7)
    @Column(name = "dest_lat")
    private BigDecimal destLat;

    @Digits(integer = 3, fraction = 7)
    @Column(name = "dest_lng")
    private BigDecimal destLng;

    
    @Column(name = "planned_start")
    private Instant plannedStart;

    
    @Column(name = "planned_end")
    private Instant plannedEnd;

    
    @Column(name = "actual_start")
    private Instant actualStart;

    
    @Column(name = "actual_end")
    private Instant actualEnd;

    @Digits(integer = 8, fraction = 2)
    @Column(name = "planned_distance_km")
    private BigDecimal plannedDistanceKm;

    @Digits(integer = 8, fraction = 2)
    @Column(name = "actual_distance_km")
    private BigDecimal actualDistanceKm;

    @Digits(integer = 8, fraction = 2)
    @Column(name = "estimated_fuel_litres")
    private BigDecimal estimatedFuelLitres;

    @Digits(integer = 8, fraction = 2)
    @Column(name = "actual_fuel_litres")
    private BigDecimal actualFuelLitres;

    
    @Column(name = "eta_at")
    private Instant etaAt;

    @NotBlank @Size(max = 20)
    @Column(name = "status")
    private String status = "PLANNED";

    
    @Column(name = "notes")
    private String notes;
}
