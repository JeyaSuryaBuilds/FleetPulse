package com.fayzo.fleetpulse.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "shipments")
public class Shipment extends BaseEntity {
    @NotBlank @Size(max = 30)
    @Column(name = "tracking_id")
    private String trackingId = com.fayzo.fleetpulse.common.IdGen.trackingId();

    
    @Column(name = "customer_id")
    private Long customerId;

    
    @Column(name = "journey_id")
    private Long journeyId;

    
    @Column(name = "vehicle_id")
    private Long vehicleId;

    
    @Column(name = "driver_id")
    private Long driverId;

    
    @Column(name = "warehouse_id")
    private Long warehouseId;

    @NotBlank @Size(max = 300)
    @Column(name = "pickup_address")
    private String pickupAddress;

    @Digits(integer = 3, fraction = 7)
    @Column(name = "pickup_lat")
    private BigDecimal pickupLat;

    @Digits(integer = 3, fraction = 7)
    @Column(name = "pickup_lng")
    private BigDecimal pickupLng;

    @NotBlank @Size(max = 300)
    @Column(name = "destination_address")
    private String destinationAddress;

    @Digits(integer = 3, fraction = 7)
    @Column(name = "dest_lat")
    private BigDecimal destLat;

    @Digits(integer = 3, fraction = 7)
    @Column(name = "dest_lng")
    private BigDecimal destLng;

    @Size(max = 160)
    @Column(name = "receiver_name")
    private String receiverName;

    @Size(max = 24)
    @Column(name = "receiver_mobile")
    private String receiverMobile;

    @Size(max = 300)
    @Column(name = "package_description")
    private String packageDescription;

    @Digits(integer = 8, fraction = 2)
    @Column(name = "weight_kg")
    private BigDecimal weightKg;

    @Digits(integer = 7, fraction = 3)
    @Column(name = "volume_cbm")
    private BigDecimal volumeCbm;

    
    @Column(name = "package_count")
    private Integer packageCount;

    @Digits(integer = 12, fraction = 2)
    @Column(name = "declared_value")
    private BigDecimal declaredValue;

    @Digits(integer = 12, fraction = 2)
    @Column(name = "price")
    private BigDecimal price;

    
    @Column(name = "scheduled_pickup")
    private Instant scheduledPickup;

    
    @Column(name = "scheduled_delivery")
    private Instant scheduledDelivery;

    
    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @NotBlank @Size(max = 30)
    @Column(name = "status")
    private String status = "CREATED";

    @Size(max = 300)
    @Column(name = "failure_reason")
    private String failureReason;

    
    @Column(name = "return_shipment")
    private Boolean returnShipment = false;

    @Size(max = 500)
    @Column(name = "pod_url")
    private String podUrl;

    @Size(max = 120)
    @Column(name = "pod_signed_by")
    private String podSignedBy;

    
    @Column(name = "notes")
    private String notes;
}
