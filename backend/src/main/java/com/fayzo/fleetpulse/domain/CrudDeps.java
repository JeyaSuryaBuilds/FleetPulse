package com.fayzo.fleetpulse.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fayzo.fleetpulse.audit.AuditService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.validation.Validator;
import org.springframework.core.convert.ConversionService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class CrudDeps {
    public final ObjectMapper mapper;
    public final Validator validator;
    public final JdbcTemplate jdbc;
    public final AuditService audit;
    public final ConversionService conversion;

    @PersistenceContext
    private EntityManager em;
    public Metamodel entityManagerMetamodel() { return em.getMetamodel(); }

    public CrudDeps(ObjectMapper mapper, Validator validator, JdbcTemplate jdbc, AuditService audit,
                    @org.springframework.beans.factory.annotation.Qualifier("mvcConversionService") ConversionService conversion) {
        this.mapper = mapper; this.validator = validator; this.jdbc = jdbc; this.audit = audit; this.conversion = conversion;
    }
}
