package com.fayzo.fleetpulse.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "branches")
public class Branch extends BaseEntity {
    @NotBlank @Size(max = 120)
    @Column(name = "name")
    private String name;

    @Size(max = 30)
    @Column(name = "code")
    private String code;

    @Size(max = 300)
    @Column(name = "address")
    private String address;

    @Size(max = 80)
    @Column(name = "city")
    private String city;

    @Size(max = 30)
    @Column(name = "phone")
    private String phone;

    @Size(max = 120)
    @Column(name = "manager_name")
    private String managerName;

    
    @Column(name = "active")
    private Boolean active = true;
}
