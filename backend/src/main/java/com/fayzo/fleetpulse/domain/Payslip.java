package com.fayzo.fleetpulse.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "payslips")
public class Payslip extends BaseEntity {
    
    @Column(name = "payroll_run_id")
    private Long payrollRunId;

    @NotNull
    @Column(name = "employee_id")
    private Long employeeId;

    @NotBlank @Size(max = 7)
    @Column(name = "period_month")
    private String periodMonth;

    @Size(max = 20)
    @Column(name = "model")
    private String model;

    @Digits(integer = 10, fraction = 2)
    @Column(name = "basic")
    private BigDecimal basic;

    @Digits(integer = 10, fraction = 2)
    @Column(name = "overtime")
    private BigDecimal overtime;

    @Digits(integer = 10, fraction = 2)
    @Column(name = "bonus")
    private BigDecimal bonus;

    @Digits(integer = 10, fraction = 2)
    @Column(name = "deductions")
    private BigDecimal deductions;

    @Digits(integer = 10, fraction = 2)
    @Column(name = "advances")
    private BigDecimal advances;

    @Digits(integer = 10, fraction = 2)
    @Column(name = "work_earnings")
    private BigDecimal workEarnings;

    @NotNull @Digits(integer = 10, fraction = 2)
    @Column(name = "net_salary")
    private BigDecimal netSalary;

    
    @Column(name = "working_days")
    private Integer workingDays;

    
    @Column(name = "present_days")
    private Integer presentDays;

    
    @Column(name = "leave_days")
    private Integer leaveDays;

    @NotBlank @Size(max = 20)
    @Column(name = "status")
    private String status = "DRAFT";

    
    @Column(name = "paid_on")
    private LocalDate paidOn;
}
