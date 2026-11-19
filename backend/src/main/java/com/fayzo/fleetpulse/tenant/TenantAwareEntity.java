package com.fayzo.fleetpulse.tenant;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

/**
 * Base class for every company-owned table. Two layers of isolation:
 *  1. @PrePersist stamps company_id from the authenticated tenant (never from request bodies).
 *  2. A Hibernate filter restricts queries to the tenant (enabled by TenantFilterAspect).
 * NOTE: Hibernate filters do not apply to EntityManager.find / findById. Services must load by id
 * through TenantGuard.owned(...) or repository methods that take companyId.
 */
@MappedSuperclass
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "companyId", type = Long.class))
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
public abstract class TenantAwareEntity {
    @JsonIgnore
    @Column(name = "company_id", nullable = false, updatable = false)
    private Long companyId;

    @PrePersist
    void stampTenant() {
        if (companyId == null) companyId = TenantContext.require();
        else if (TenantContext.get() != null && !companyId.equals(TenantContext.get()))
            throw new IllegalStateException("Cross-tenant write blocked");
    }
    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long companyId) { this.companyId = companyId; }
}
