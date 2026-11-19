package com.fayzo.fleetpulse.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "expenses")
public class Expense extends BaseEntity {
    @NotBlank @Size(max = 40)
    @Column(name = "category")
    private String category;

    @NotNull @Digits(integer = 12, fraction = 2)
    @Column(name = "amount")
    private BigDecimal amount;

    @NotNull
    @Column(name = "expense_date")
    private LocalDate expenseDate;

    
    @Column(name = "vehicle_id")
    private Long vehicleId;

    
    @Column(name = "vendor_id")
    private Long vendorId;

    @Size(max = 300)
    @Column(name = "description")
    private String description;
}
