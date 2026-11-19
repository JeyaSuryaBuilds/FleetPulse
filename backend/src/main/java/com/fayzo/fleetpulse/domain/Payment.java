package com.fayzo.fleetpulse.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payments")
public class Payment extends BaseEntity {
    
    @Column(name = "invoice_id")
    private Long invoiceId;

    @NotBlank @Size(max = 10)
    @Column(name = "direction")
    private String direction;

    @NotNull @Digits(integer = 12, fraction = 2)
    @Column(name = "amount")
    private BigDecimal amount;

    @Size(max = 30)
    @Column(name = "method")
    private String method;

    @Size(max = 120)
    @Column(name = "reference")
    private String reference;

    @NotNull
    @Column(name = "paid_at")
    private Instant paidAt;

    @Size(max = 120)
    @Column(name = "gateway_ref")
    private String gatewayRef;
}
