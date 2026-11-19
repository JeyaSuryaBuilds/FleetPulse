package com.fayzo.fleetpulse.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "customer_feedback")
public class CustomerFeedback extends BaseEntity {
    
    @Column(name = "customer_id")
    private Long customerId;

    
    @Column(name = "shipment_id")
    private Long shipmentId;

    @NotBlank @Size(max = 20)
    @Column(name = "kind")
    private String kind = "FEEDBACK";

    
    @Column(name = "rating")
    private Integer rating;

    
    @Column(name = "comment")
    private String comment;

    @NotBlank @Size(max = 20)
    @Column(name = "status")
    private String status = "OPEN";
}
