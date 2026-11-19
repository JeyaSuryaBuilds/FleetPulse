package com.fayzo.fleetpulse.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "departments")
public class Department extends BaseEntity {
    @NotBlank @Size(max = 120)
    @Column(name = "name")
    private String name;

    
    @Column(name = "branch_id")
    private Long branchId;

    @Size(max = 300)
    @Column(name = "description")
    private String description;
}
