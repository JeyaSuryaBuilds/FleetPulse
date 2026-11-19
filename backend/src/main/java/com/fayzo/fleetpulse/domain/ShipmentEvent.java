package com.fayzo.fleetpulse.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "shipment_events")
public class ShipmentEvent extends BaseEntity {
    @NotNull
    @Column(name = "shipment_id")
    private Long shipmentId;

    @NotBlank @Size(max = 30)
    @Column(name = "status")
    private String status;

    @Size(max = 300)
    @Column(name = "note")
    private String note;

    @Digits(integer = 3, fraction = 7)
    @Column(name = "lat")
    private BigDecimal lat;

    @Digits(integer = 3, fraction = 7)
    @Column(name = "lng")
    private BigDecimal lng;

    @NotNull
    @Column(name = "occurred_at")
    private Instant occurredAt;

    
    @Column(name = "public_visible")
    private Boolean publicVisible = true;
}
