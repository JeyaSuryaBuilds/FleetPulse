package com.fayzo.fleetpulse.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "attendance")
public class Attendance extends BaseEntity {
    @NotNull
    @Column(name = "employee_id")
    private Long employeeId;

    @NotNull
    @Column(name = "work_date")
    private LocalDate workDate;

    
    @Column(name = "check_in")
    private Instant checkIn;

    
    @Column(name = "check_out")
    private Instant checkOut;

    @NotBlank @Size(max = 20)
    @Column(name = "status")
    private String status = "PRESENT";

    @Digits(integer = 3, fraction = 7)
    @Column(name = "lat")
    private BigDecimal lat;

    @Digits(integer = 3, fraction = 7)
    @Column(name = "lng")
    private BigDecimal lng;

    @Size(max = 300)
    @Column(name = "notes")
    private String notes;
}
