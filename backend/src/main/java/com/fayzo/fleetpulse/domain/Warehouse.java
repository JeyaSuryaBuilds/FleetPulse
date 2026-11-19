package com.fayzo.fleetpulse.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "warehouses")
public class Warehouse extends BaseEntity {
    @NotBlank @Size(max = 120)
    @Column(name = "name")
    private String name;

    @Size(max = 30)
    @Column(name = "code")
    private String code;

    
    @Column(name = "branch_id")
    private Long branchId;

    @Size(max = 300)
    @Column(name = "address")
    private String address;

    @Size(max = 80)
    @Column(name = "city")
    private String city;

    
    @Column(name = "capacity")
    private Integer capacity;

    
    @Column(name = "active")
    private Boolean active = true;
}
