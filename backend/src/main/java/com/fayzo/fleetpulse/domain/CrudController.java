package com.fayzo.fleetpulse.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fayzo.fleetpulse.common.ApiException;
import com.fayzo.fleetpulse.common.Authz;
import com.fayzo.fleetpulse.security.AuthPrincipal;
import com.fayzo.fleetpulse.security.Permission;
import com.fayzo.fleetpulse.tenant.TenantContext;
import com.fayzo.fleetpulse.tenant.TenantGuard;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Generic tenant-safe CRUD. Every call runs inside a transaction (required so the tenant filter is active),
 * checks the role permission, rejects references to other tenants' records, and writes an audit entry.
 * Filtering: any entity attribute can be used as an equality query parameter (e.g. ?status=ACTIVE&vehicleId=3);
 * ?q= searches the text columns. Sorting: ?sort=field,desc. Page size is capped at 100.
 */
@Transactional
public abstract class CrudController<E extends BaseEntity, R extends JpaRepository<E, Long> & JpaSpecificationExecutor<E>> {
    private final Class<E> type;
    private final R repo;
    private final CrudDeps d;
    private final Permission readPerm;
    private final Permission writePerm;
    private final Map<String, String> fks;      // field -> referenced table (must belong to same company)
    private final String entityName;

    protected CrudController(Class<E> type, R repo, CrudDeps d, Permission readPerm, Permission writePerm,
                             Map<String, String> fks, String entityName) {
        this.type = type; this.repo = repo; this.d = d; this.readPerm = readPerm; this.writePerm = writePerm;
        this.fks = fks; this.entityName = entityName;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public Page<E> list(@RequestParam Map<String, String> params,
                        @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "25") int size,
                        @RequestParam(required = false) String sort) {
        Authz.require(readPerm);
        Sort s = Sort.by(Sort.Direction.DESC, "id");
        if (sort != null && !sort.isBlank()) {
            String[] p = sort.split(",");
            Set<String> attrs = attributeNames();
            if (!attrs.contains(p[0])) throw ApiException.badRequest("BAD_SORT", "Unknown sort field");
            s = Sort.by(p.length > 1 && p[1].equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, p[0]);
        }
        Pageable pg = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100), s);
        return repo.findAll(filterSpec(params), pg);
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public E get(@PathVariable long id) {
        Authz.require(readPerm);
        return TenantGuard.owned(repo.findById(id), entityName);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public E create(@RequestBody JsonNode body, HttpServletRequest http) throws IOException {
        Authz.require(writePerm);
        E e = d.mapper.treeToValue(body, type);
        validate(e);
        checkReferences(e);
        beforeCreate(e, body);
        E saved = repo.save(e);
        audit("CREATE", saved, http);
        return saved;
    }

    @RequestMapping(value = "/{id}", method = {RequestMethod.PUT, RequestMethod.PATCH})
    public E update(@PathVariable long id, @RequestBody JsonNode body, HttpServletRequest http) throws IOException {
        Authz.require(writePerm);
        E existing = TenantGuard.owned(repo.findById(id), entityName);
        beforeUpdate(existing, body);
        d.mapper.readerForUpdating(existing).readValue(body);
        validate(existing);
        checkReferences(existing);
        E saved = repo.save(existing);
        audit("UPDATE", saved, http);
        return saved;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id, HttpServletRequest http) {
        Authz.require(writePerm);
        E existing = TenantGuard.owned(repo.findById(id), entityName);
        repo.delete(existing);
        repo.flush();
        audit("DELETE", existing, http);
    }

    /** Hook for plan limits and module-specific rules. */
    protected void beforeCreate(E e, JsonNode body) {}

    /** Hook to reject or normalise edits (e.g. status changes that must go through a workflow endpoint). */
    protected void beforeUpdate(E existing, JsonNode body) {}

    // ---- helpers ----
    private void validate(E e) {
        Set<ConstraintViolation<E>> v = d.validator.validate(e);
        if (!v.isEmpty()) throw new ConstraintViolationException(v);
    }

    /** Blocks references to records owned by another company (IDOR / cross-tenant linking). */
    private void checkReferences(E e) {
        if (fks.isEmpty()) return;
        JsonNode node = d.mapper.valueToTree(e);
        long company = TenantContext.require();
        for (var fk : fks.entrySet()) {
            JsonNode v = node.get(fk.getKey());
            if (v == null || v.isNull()) continue;
            Integer n = d.jdbc.queryForObject("SELECT COUNT(*) FROM " + fk.getValue() + " WHERE id=? AND company_id=?", Integer.class, v.asLong(), company);
            if (n == null || n == 0) throw ApiException.badRequest("INVALID_REFERENCE", "Referenced " + fk.getKey() + " does not exist");
        }
    }

    private void audit(String action, E e, HttpServletRequest http) {
        AuthPrincipal p = Authz.principal();
        d.audit.log(p.companyId(), p.type(), p.subjectId(), action + "_" + entityName.toUpperCase().replace(' ', '_'), entityName,
                String.valueOf(e.getId()), http.getRemoteAddr());
    }

    private Set<String> attributeNames() {
        Set<String> out = new java.util.HashSet<>();
        for (var a : d.entityManagerMetamodel().entity(type).getAttributes()) out.add(a.getName());
        return out;
    }

    private Specification<E> filterSpec(Map<String, String> params) {
        Set<String> attrs = attributeNames();
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            for (var en : params.entrySet()) {
                String k = en.getKey();
                if (k.equals("page") || k.equals("size") || k.equals("sort") || k.equals("q") || !attrs.contains(k) || k.equals("companyId")) continue;
                var path = root.get(k);
                Object val = d.conversion.convert(en.getValue(), path.getJavaType());
                ps.add(cb.equal(path, val));
            }
            String q = params.get("q");
            if (q != null && !q.isBlank()) {
                List<Predicate> ors = new ArrayList<>();
                for (var a : root.getModel().getAttributes())
                    if (a.getJavaType() == String.class)
                        ors.add(cb.like(cb.lower(root.get(a.getName())), "%" + q.toLowerCase().replace("%", "").replace("_", "") + "%"));
                if (!ors.isEmpty()) ps.add(cb.or(ors.toArray(new Predicate[0])));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
    }
}
