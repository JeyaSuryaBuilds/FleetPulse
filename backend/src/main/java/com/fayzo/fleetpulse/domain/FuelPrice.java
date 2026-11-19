package com.fayzo.fleetpulse.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "fuel_prices")
public class FuelPrice extends BaseEntity {
    @NotBlank @Size(max = 20)
    @Column(name = "fuel_type")
    private String fuelType;

    @NotNull @Digits(integer = 6, fraction = 4)
    @Column(name = "price_per_litre")
    private BigDecimal pricePerLitre;

    @NotNull
    @Column(name = "effective_from")
    private LocalDate effectiveFrom;

    @Size(max = 80)
    @Column(name = "region")
    private String region;
}
