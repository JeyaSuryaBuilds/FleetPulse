package com.fayzo.fleetpulse.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "vendors")
public class Vendor extends BaseEntity {
    @NotBlank @Size(max = 160)
    @Column(name = "name")
    private String name;

    @NotBlank @Size(max = 30)
    @Column(name = "vendor_type")
    private String vendorType;

    @Size(max = 120)
    @Column(name = "contact_name")
    private String contactName;

    @Size(max = 190)
    @Column(name = "email")
    private String email;

    @Size(max = 24)
    @Column(name = "mobile")
    private String mobile;

    @Size(max = 300)
    @Column(name = "address")
    private String address;

    @Size(max = 60)
    @Column(name = "tax_id")
    private String taxId;

    
    @Column(name = "active")
    private Boolean active = true;
}
