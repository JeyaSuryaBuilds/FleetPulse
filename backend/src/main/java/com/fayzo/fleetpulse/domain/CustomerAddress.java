package com.fayzo.fleetpulse.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Entity
@Table(name = "customer_addresses")
public class CustomerAddress extends BaseEntity {
    @NotNull
    @Column(name = "customer_id")
    private Long customerId;

    @Size(max = 60)
    @Column(name = "label")
    private String label;

    @NotBlank @Size(max = 200)
    @Column(name = "line1")
    private String line1;

    @Size(max = 80)
    @Column(name = "city")
    private String city;

    @Size(max = 80)
    @Column(name = "state")
    private String state;

    @Size(max = 20)
    @Column(name = "postal_code")
    private String postalCode;

    @Size(max = 80)
    @Column(name = "country")
    private String country;

    @Digits(integer = 3, fraction = 7)
    @Column(name = "latitude")
    private BigDecimal latitude;

    @Digits(integer = 3, fraction = 7)
    @Column(name = "longitude")
    private BigDecimal longitude;
}
