package com.fayzo.fleetpulse.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Entity
@Table(name = "geofences")
public class Geofence extends BaseEntity {
    @NotBlank @Size(max = 120)
    @Column(name = "name")
    private String name;

    @NotNull @Digits(integer = 3, fraction = 7)
    @Column(name = "center_lat")
    private BigDecimal centerLat;

    @NotNull @Digits(integer = 3, fraction = 7)
    @Column(name = "center_lng")
    private BigDecimal centerLng;

    @NotNull
    @Column(name = "radius_meters")
    private Integer radiusMeters;

    
    @Column(name = "active")
    private Boolean active = true;
}
