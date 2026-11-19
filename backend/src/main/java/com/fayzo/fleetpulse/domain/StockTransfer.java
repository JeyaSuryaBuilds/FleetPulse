package com.fayzo.fleetpulse.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.Instant;

@Entity
@Table(name = "stock_transfers")
public class StockTransfer extends BaseEntity {
    @NotNull
    @Column(name = "from_warehouse_id")
    private Long fromWarehouseId;

    @NotNull
    @Column(name = "to_warehouse_id")
    private Long toWarehouseId;

    @NotNull
    @Column(name = "item_id")
    private Long itemId;

    @NotNull
    @Column(name = "quantity")
    private Integer quantity;

    @NotBlank @Size(max = 20)
    @Column(name = "status")
    private String status = "COMPLETED";

    
    @Column(name = "transferred_at")
    private Instant transferredAt;
}
