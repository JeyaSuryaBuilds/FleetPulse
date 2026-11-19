package com.fayzo.fleetpulse.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "inventory_items")
public class InventoryItem extends BaseEntity {
    @NotNull
    @Column(name = "warehouse_id")
    private Long warehouseId;

    @NotBlank @Size(max = 60)
    @Column(name = "sku")
    private String sku;

    @NotBlank @Size(max = 160)
    @Column(name = "name")
    private String name;

    @Size(max = 80)
    @Column(name = "barcode")
    private String barcode;

    
    @Column(name = "quantity")
    private Integer quantity = 0;

    @Size(max = 20)
    @Column(name = "unit")
    private String unit;

    
    @Column(name = "reorder_level")
    private Integer reorderLevel;
}
