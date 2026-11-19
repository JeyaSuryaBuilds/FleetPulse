package com.fayzo.fleetpulse.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.Instant;

@Entity
@Table(name = "notifications")
public class Notification extends BaseEntity {
    
    @Column(name = "user_id")
    private Long userId;

    @NotBlank @Size(max = 40)
    @Column(name = "type")
    private String type;

    @NotBlank @Size(max = 10)
    @Column(name = "severity")
    private String severity = "INFO";

    @NotBlank @Size(max = 200)
    @Column(name = "title")
    private String title;

    
    @Column(name = "message")
    private String message;

    @Size(max = 40)
    @Column(name = "entity_type")
    private String entityType;

    
    @Column(name = "entity_id")
    private Long entityId;

    
    @Column(name = "read_at")
    private Instant readAt;
}
