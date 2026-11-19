package com.fayzo.fleetpulse.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "support_tickets")
public class SupportTicket extends BaseEntity {
    @NotBlank @Size(max = 20)
    @Column(name = "category")
    private String category = "SUPPORT";

    @NotBlank @Size(max = 200)
    @Column(name = "subject")
    private String subject;

    
    @Column(name = "description")
    private String description;

    @NotBlank @Size(max = 20)
    @Column(name = "status")
    private String status = "OPEN";

    @NotBlank @Size(max = 10)
    @Column(name = "priority")
    private String priority = "NORMAL";

    
    @Column(name = "created_by_user_id")
    private Long createdByUserId;
}
