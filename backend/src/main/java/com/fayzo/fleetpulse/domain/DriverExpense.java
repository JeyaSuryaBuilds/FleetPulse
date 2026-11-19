package com.fayzo.fleetpulse.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "driver_expenses")
public class DriverExpense extends BaseEntity {
    @NotNull
    @Column(name = "driver_id")
    private Long driverId;

    
    @Column(name = "journey_id")
    private Long journeyId;

    @NotNull
    @Column(name = "expense_date")
    private LocalDate expenseDate;

    @Size(max = 40)
    @Column(name = "category")
    private String category;

    @NotNull @Digits(integer = 10, fraction = 2)
    @Column(name = "amount")
    private BigDecimal amount;

    @Size(max = 300)
    @Column(name = "description")
    private String description;
}
