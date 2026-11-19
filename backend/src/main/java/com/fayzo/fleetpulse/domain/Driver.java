package com.fayzo.fleetpulse.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

@Entity
@Table(name = "drivers")
public class Driver extends BaseEntity {
    @NotBlank @Size(max = 160)
    @Column(name = "full_name")
    private String fullName;

    @Size(max = 24)
    @Column(name = "mobile")
    private String mobile;

    @Size(max = 190)
    @Column(name = "email")
    private String email;

    
    @Column(name = "user_id")
    private Long userId;

    @Size(max = 40)
    @Column(name = "licence_number")
    private String licenceNumber;

    @Size(max = 20)
    @Column(name = "licence_class")
    private String licenceClass;

    
    @Column(name = "licence_expiry")
    private LocalDate licenceExpiry;

    @NotBlank @Size(max = 20)
    @Column(name = "status")
    private String status = "AVAILABLE";

    
    @Column(name = "branch_id")
    private Long branchId;

    
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Size(max = 300)
    @Column(name = "address")
    private String address;

    
    @Column(name = "joined_on")
    private LocalDate joinedOn;
}
