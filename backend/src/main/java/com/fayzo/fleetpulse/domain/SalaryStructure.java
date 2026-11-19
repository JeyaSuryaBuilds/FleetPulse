package com.fayzo.fleetpulse.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "salary_structures")
public class SalaryStructure extends BaseEntity {
    @NotNull
    @Column(name = "employee_id")
    private Long employeeId;

    @NotBlank @Size(max = 20)
    @Column(name = "model")
    private String model = "FIXED";

    @Digits(integer = 10, fraction = 2)
    @Column(name = "monthly_salary")
    private BigDecimal monthlySalary;

    @Digits(integer = 10, fraction = 2)
    @Column(name = "overtime_rate")
    private BigDecimal overtimeRate;

    @Digits(integer = 10, fraction = 2)
    @Column(name = "per_delivery_rate")
    private BigDecimal perDeliveryRate;

    @Digits(integer = 10, fraction = 2)
    @Column(name = "per_trip_rate")
    private BigDecimal perTripRate;

    @Digits(integer = 8, fraction = 4)
    @Column(name = "per_km_rate")
    private BigDecimal perKmRate;

    @Digits(integer = 10, fraction = 2)
    @Column(name = "per_task_rate")
    private BigDecimal perTaskRate;

    
    @Column(name = "effective_from")
    private LocalDate effectiveFrom;
}
