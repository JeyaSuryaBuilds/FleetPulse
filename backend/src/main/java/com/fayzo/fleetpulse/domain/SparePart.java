package com.fayzo.fleetpulse.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Entity
@Table(name = "spare_parts")
public class SparePart extends BaseEntity {
    @NotBlank @Size(max = 160)
    @Column(name = "name")
    private String name;

    @Size(max = 60)
    @Column(name = "sku")
    private String sku;

    
    @Column(name = "vendor_id")
    private Long vendorId;

    
    @Column(name = "quantity")
    private Integer quantity = 0;

    @Digits(integer = 10, fraction = 2)
    @Column(name = "unit_cost")
    private BigDecimal unitCost;

    
    @Column(name = "reorder_level")
    private Integer reorderLevel;
}
