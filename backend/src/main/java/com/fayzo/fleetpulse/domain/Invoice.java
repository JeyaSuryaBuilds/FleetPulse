package com.fayzo.fleetpulse.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "invoices")
public class Invoice extends BaseEntity {
    @NotBlank @Size(max = 30)
    @Column(name = "invoice_number")
    private String invoiceNumber = com.fayzo.fleetpulse.common.IdGen.code("INV");

    @NotBlank @Size(max = 10)
    @Column(name = "kind")
    private String kind;

    
    @Column(name = "customer_id")
    private Long customerId;

    
    @Column(name = "vendor_id")
    private Long vendorId;

    
    @Column(name = "shipment_id")
    private Long shipmentId;

    @NotNull
    @Column(name = "issue_date")
    private LocalDate issueDate;

    
    @Column(name = "due_date")
    private LocalDate dueDate;

    @Digits(integer = 12, fraction = 2)
    @Column(name = "subtotal")
    private BigDecimal subtotal;

    @Digits(integer = 12, fraction = 2)
    @Column(name = "tax")
    private BigDecimal tax;

    @NotNull @Digits(integer = 12, fraction = 2)
    @Column(name = "total")
    private BigDecimal total;

    @NotBlank @Size(max = 20)
    @Column(name = "status")
    private String status = "DRAFT";

    @Size(max = 3)
    @Column(name = "currency")
    private String currency;

    
    @Column(name = "notes")
    private String notes;
}
