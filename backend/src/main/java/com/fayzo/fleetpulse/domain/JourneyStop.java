package com.fayzo.fleetpulse.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "journey_stops")
public class JourneyStop extends BaseEntity {
    @NotNull
    @Column(name = "journey_id")
    private Long journeyId;

    @NotNull
    @Column(name = "sequence_no")
    private Integer sequenceNo;

    @NotBlank @Size(max = 300)
    @Column(name = "location")
    private String location;

    @Digits(integer = 3, fraction = 7)
    @Column(name = "lat")
    private BigDecimal lat;

    @Digits(integer = 3, fraction = 7)
    @Column(name = "lng")
    private BigDecimal lng;

    
    @Column(name = "planned_arrival")
    private Instant plannedArrival;

    
    @Column(name = "actual_arrival")
    private Instant actualArrival;

    @NotBlank @Size(max = 20)
    @Column(name = "status")
    private String status = "PENDING";
}
