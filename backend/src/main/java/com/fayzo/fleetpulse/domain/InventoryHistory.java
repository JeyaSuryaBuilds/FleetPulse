package com.fayzo.fleetpulse.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.Instant;

@Entity
@Table(name = "inventory_history")
public class InventoryHistory extends BaseEntity {
    @NotNull
    @Column(name = "item_id")
    private Long itemId;

    @NotNull
    @Column(name = "change_qty")
    private Integer changeQty;

    @Size(max = 60)
    @Column(name = "reason")
    private String reason;

    @Size(max = 40)
    @Column(name = "ref_type")
    private String refType;

    
    @Column(name = "ref_id")
    private Long refId;

    
    @Column(name = "balance_after")
    private Integer balanceAfter;

    @NotNull
    @Column(name = "occurred_at")
    private Instant occurredAt;
}
