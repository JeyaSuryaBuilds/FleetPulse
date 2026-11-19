package com.fayzo.fleetpulse.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "customers")
public class Customer extends BaseEntity {
    @NotBlank @Size(max = 160)
    @Column(name = "name")
    private String name;

    @Size(max = 190)
    @Column(name = "email")
    private String email;

    @Size(max = 24)
    @Column(name = "mobile")
    private String mobile;

    @Size(max = 160)
    @Column(name = "company_name")
    private String companyName;

    @Size(max = 60)
    @Column(name = "tax_id")
    private String taxId;

    @NotBlank @Size(max = 20)
    @Column(name = "status")
    private String status = "ACTIVE";

    
    @Column(name = "notes")
    private String notes;
}
