package com.fayzo.fleetpulse.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "fuel_records")
public class FuelRecord extends BaseEntity {
    @NotNull
    @Column(name = "vehicle_id")
    private Long vehicleId;

    
    @Column(name = "driver_id")
    private Long driverId;

    
    @Column(name = "journey_id")
    private Long journeyId;

    
    @Column(name = "vendor_id")
    private Long vendorId;

    @NotNull
    @Column(name = "fuel_date")
    private Instant fuelDate;

    @NotNull @Digits(integer = 8, fraction = 2)
    @Column(name = "litres")
    private BigDecimal litres;

    @Digits(integer = 6, fraction = 4)
    @Column(name = "price_per_litre")
    private BigDecimal pricePerLitre;

    @NotNull @Digits(integer = 10, fraction = 2)
    @Column(name = "total_cost")
    private BigDecimal totalCost;

    @Digits(integer = 11, fraction = 1)
    @Column(name = "odometer_km")
    private BigDecimal odometerKm;

    @Size(max = 120)
    @Column(name = "station")
    private String station;

    @Size(max = 500)
    @Column(name = "receipt_url")
    private String receiptUrl;
}
