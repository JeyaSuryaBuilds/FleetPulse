package com.fayzo.fleetpulse.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Entity
@Table(name = "payroll_runs")
public class PayrollRun extends BaseEntity {
    @NotBlank @Size(max = 7)
    @Column(name = "period_month")
    private String periodMonth;

    @NotBlank @Size(max = 20)
    @Column(name = "status")
    private String status = "DRAFT";

    @Digits(integer = 12, fraction = 2)
    @Column(name = "total_net")
    private BigDecimal totalNet;
}
