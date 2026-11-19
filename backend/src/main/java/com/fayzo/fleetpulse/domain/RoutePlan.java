package com.fayzo.fleetpulse.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Entity
@Table(name = "routes")
public class RoutePlan extends BaseEntity {
    @NotBlank @Size(max = 160)
    @Column(name = "name")
    private String name;

    
    @Column(name = "journey_id")
    private Long journeyId;

    @Size(max = 300)
    @Column(name = "origin_name")
    private String originName;

    @Size(max = 300)
    @Column(name = "destination_name")
    private String destinationName;

    @NotBlank @Size(max = 20)
    @Column(name = "mode")
    private String mode = "FASTEST";

    @Digits(integer = 8, fraction = 2)
    @Column(name = "distance_km")
    private BigDecimal distanceKm;

    
    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Digits(integer = 10, fraction = 2)
    @Column(name = "toll_estimate")
    private BigDecimal tollEstimate;

    @Digits(integer = 8, fraction = 2)
    @Column(name = "fuel_estimate_litres")
    private BigDecimal fuelEstimateLitres;

    
    @Column(name = "waypoints_json")
    private String waypointsJson;

    @Size(max = 60)
    @Column(name = "computed_by")
    private String computedBy;
}
