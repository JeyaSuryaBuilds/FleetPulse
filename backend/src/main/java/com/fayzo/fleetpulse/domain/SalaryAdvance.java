package com.fayzo.fleetpulse.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "salary_advances")
public class SalaryAdvance extends BaseEntity {
    @NotNull
    @Column(name = "employee_id")
    private Long employeeId;

    @NotNull @Digits(integer = 10, fraction = 2)
    @Column(name = "amount")
    private BigDecimal amount;

    @NotNull
    @Column(name = "advance_date")
    private LocalDate advanceDate;

    @Digits(integer = 10, fraction = 2)
    @Column(name = "repaid")
    private BigDecimal repaid = new BigDecimal("0");

    @NotBlank @Size(max = 20)
    @Column(name = "status")
    private String status = "OPEN";
}
