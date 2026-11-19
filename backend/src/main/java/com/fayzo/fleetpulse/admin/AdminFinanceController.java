package com.fayzo.fleetpulse.admin;

import com.fayzo.fleetpulse.audit.AuditService;
import com.fayzo.fleetpulse.common.ApiException;
import com.fayzo.fleetpulse.security.AuthPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Platform finance views. Figures come from platform_invoices / platform_payments, which are written by the payment
 * gateway webhook integration (not yet built). Until a gateway is connected these tables are simply empty: nothing is invented.
 */
@RestController
@RequestMapping("/api/v1/admin/finance")
public class AdminFinanceController {
    private final JdbcTemplate jdbc;
    private final AuditService audit;
    public AdminFinanceController(JdbcTemplate jdbc, AuditService audit) { this.jdbc = jdbc; this.audit = audit; }

    @GetMapping("/payments")
    public Map<String, Object> payments(@RequestParam(required = false) String status, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "25") int size) {
        int sz = Page.size(size), pg = Math.max(page, 0);
        String where = status == null || status.isBlank() ? "" : " WHERE p.status=?";
        Object[] a = status == null || status.isBlank() ? new Object[]{} : new Object[]{status};
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM platform_payments p" + where, Long.class, a);
        List<Object> a2 = new ArrayList<>(List.of(a)); a2.add(sz); a2.add(pg * sz);
        return Page.of(jdbc.queryForList("""
            SELECT p.id, c.name AS company, p.amount, p.currency, p.status, p.gateway, p.gateway_ref AS gatewayRef, p.failure_reason AS failureReason, p.created_at AS createdAt
            FROM platform_payments p JOIN companies c ON c.id=p.company_id""" + where + " ORDER BY p.id DESC LIMIT ? OFFSET ?", a2.toArray()), pg, sz, total == null ? 0 : total);
    }

    @GetMapping("/invoices")
    public List<Map<String, Object>> invoices(@RequestParam(required = false) String status) {
        if (status == null || status.isBlank())
            return jdbc.queryForList("SELECT i.id, c.name AS company, i.invoice_number AS invoiceNumber, i.total, i.tax, i.currency, i.status, i.issued_at AS issuedAt FROM platform_invoices i JOIN companies c ON c.id=i.company_id ORDER BY i.id DESC LIMIT 200");
        return jdbc.queryForList("SELECT i.id, c.name AS company, i.invoice_number AS invoiceNumber, i.total, i.tax, i.currency, i.status, i.issued_at AS issuedAt FROM platform_invoices i JOIN companies c ON c.id=i.company_id WHERE i.status=? ORDER BY i.id DESC LIMIT 200", status);
    }

    /** Records a refund REQUEST. Actual money movement must be executed in the payment gateway; a webhook then marks it REFUNDED. */
    @PostMapping("/payments/{id}/refund-request")
    public Map<String, Boolean> requestRefund(@PathVariable long id, @AuthenticationPrincipal AuthPrincipal admin, HttpServletRequest http) {
        int n = jdbc.update("UPDATE platform_payments SET status='REFUND_REQUESTED' WHERE id=? AND status='SUCCEEDED'", id);
        if (n == 0) throw ApiException.badRequest("NOT_REFUNDABLE", "Only succeeded payments can be refunded");
        audit.log(null, "ADMIN", admin.subjectId(), "REFUND_REQUESTED", "platform_payment", String.valueOf(id), http.getRemoteAddr());
        return Map.of("ok", true);
    }

    @GetMapping("/tax-report")
    public List<Map<String, Object>> taxReport() {
        return jdbc.queryForList("""
            SELECT DATE_FORMAT(issued_at,'%Y-%m') AS month, currency, SUM(subtotal) AS subtotal, SUM(tax) AS tax, SUM(total) AS total, COUNT(*) AS invoices
            FROM platform_invoices WHERE status IN ('PAID','OPEN') GROUP BY DATE_FORMAT(issued_at,'%Y-%m'), currency ORDER BY month DESC LIMIT 36""");
    }
}
