package com.fayzo.fleetpulse.tenant;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.Session;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Enables the tenant filter on the transaction's Hibernate session for every repository call.
 * Runs inside the transaction advice (lowest precedence) so it targets the right session.
 */
@Aspect
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class TenantFilterAspect {
    @PersistenceContext
    private EntityManager em;

    @Around("execution(* org.springframework.data.repository.Repository+.*(..))")
    public Object apply(ProceedingJoinPoint pjp) throws Throwable {
        Long tenant = TenantContext.get();
        if (tenant != null && em.isJoinedToTransaction()) {
            Session s = em.unwrap(Session.class);
            s.enableFilter("tenantFilter").setParameter("companyId", tenant);
        }
        return pjp.proceed();
    }
}
