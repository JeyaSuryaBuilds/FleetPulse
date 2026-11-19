package com.fayzo.fleetpulse.domain;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fayzo.fleetpulse.tenant.TenantAwareEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Common columns for tenant-owned tables. JSON uses field access; id and timestamps are read-only on input and
 * company_id is never accepted from clients (ignored in TenantAwareEntity, stamped from the authenticated tenant).
 */
@MappedSuperclass
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public abstract class BaseEntity extends TenantAwareEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Instant updatedAt;

    public Long getId() { return id; }
}
