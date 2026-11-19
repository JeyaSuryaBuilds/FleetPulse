package com.fayzo.fleetpulse.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "work_records")
public class WorkRecord extends BaseEntity {
    @NotNull
    @Column(name = "employee_id")
    private Long employeeId;

    @NotBlank @Size(max = 20)
    @Column(name = "work_type")
    private String workType;

    
    @Column(name = "reference_id")
    private Long referenceId;

    @NotNull @Digits(integer = 8, fraction = 2)
    @Column(name = "quantity")
    private BigDecimal quantity;

    @Digits(integer = 10, fraction = 2)
    @Column(name = "rate")
    private BigDecimal rate;

    @Digits(integer = 10, fraction = 2)
    @Column(name = "amount")
    private BigDecimal amount;

    @NotNull
    @Column(name = "work_date")
    private LocalDate workDate;

    @NotBlank @Size(max = 20)
    @Column(name = "status")
    private String status = "PENDING";

    
    @Column(name = "approved_by")
    private Long approvedBy;
}
