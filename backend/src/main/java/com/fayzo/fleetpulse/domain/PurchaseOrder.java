package com.fayzo.fleetpulse.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "purchase_orders")
public class PurchaseOrder extends BaseEntity {
    @NotBlank @Size(max = 30)
    @Column(name = "po_number")
    private String poNumber = com.fayzo.fleetpulse.common.IdGen.code("PO");

    @NotNull
    @Column(name = "vendor_id")
    private Long vendorId;

    @NotNull
    @Column(name = "order_date")
    private LocalDate orderDate;

    @NotBlank @Size(max = 20)
    @Column(name = "status")
    private String status = "DRAFT";

    @Digits(integer = 12, fraction = 2)
    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    
    @Column(name = "notes")
    private String notes;
}
