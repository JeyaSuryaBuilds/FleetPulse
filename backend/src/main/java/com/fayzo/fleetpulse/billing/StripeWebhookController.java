package com.fayzo.fleetpulse.billing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fayzo.fleetpulse.common.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Stripe webhook: the only code path that marks subscriptions paid or writes platform payments. Signature-verified, idempotent per event id.
 * Handled: checkout.session.completed, invoice.paid, invoice.payment_failed, customer.subscription.deleted.
 * Must be configured in the Stripe dashboard to POST to /api/v1/public/webhooks/stripe.
 */
@RestController
@RequestMapping("/api/v1/public/webhooks")
public class StripeWebhookController {
    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);
    private final StripeBilling stripe;
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    public StripeWebhookController(StripeBilling stripe, JdbcTemplate jdbc, ObjectMapper mapper) { this.stripe = stripe; this.jdbc = jdbc; this.mapper = mapper; }

    @PostMapping("/stripe")
    @Transactional
    public Map<String, Boolean> stripe(@RequestBody String payload, @RequestHeader(value = "Stripe-Signature", required = false) String sig) throws Exception {
        if (!stripe.verify(payload, sig)) throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_SIGNATURE", "Invalid signature");
        JsonNode ev = mapper.readTree(payload);
        try { jdbc.update("INSERT INTO processed_webhook_events(event_id,provider) VALUES (?,'stripe')", ev.path("id").asText()); }
        catch (DuplicateKeyException e) { return Map.of("duplicate", true); }
        JsonNode obj = ev.path("data").path("object");
        switch (ev.path("type").asText()) {
            case "checkout.session.completed" -> completed(obj);
            case "invoice.paid" -> invoice(obj, true);
            case "invoice.payment_failed" -> invoice(obj, false);
            case "customer.subscription.deleted" -> deleted(obj);
            default -> log.debug("Ignored Stripe event {}", ev.path("type").asText());
        }
        return Map.of("ok", true);
    }

    private void completed(JsonNode s) {
        long company = s.path("metadata").path("company_id").asLong(0);
        String plan = s.path("metadata").path("plan_code").asText(""), cycle = s.path("metadata").path("billing_cycle").asText("MONTHLY");
        if (company == 0) return;
        List<Long> planIds = jdbc.queryForList("SELECT id FROM plans WHERE code=?", Long.class, plan);
        List<Long> subs = jdbc.queryForList("SELECT id FROM subscriptions WHERE company_id=? ORDER BY id DESC LIMIT 1", Long.class, company);
        if (planIds.isEmpty() || subs.isEmpty()) return;
        String interval = "ANNUAL".equals(cycle) ? "YEAR" : "MONTH";
        jdbc.update("UPDATE subscriptions SET plan_id=?, status='ACTIVE', billing_cycle=?, trial_ends_at=NULL, cancel_at_period_end=FALSE, cancelled_at=NULL, gateway_customer_ref=?, gateway_subscription_ref=?, current_period_start=UTC_TIMESTAMP(), current_period_end=DATE_ADD(UTC_TIMESTAMP(), INTERVAL 1 " + interval + ") WHERE id=?",
                planIds.get(0), cycle, s.path("customer").asText(null), s.path("subscription").asText(null), subs.get(0));
        jdbc.update("UPDATE companies SET status='ACTIVE' WHERE id=? AND status IN ('TRIAL','EXPIRED')", company);
    }

    private void invoice(JsonNode inv, boolean paid) {
        String subRef = inv.path("subscription").asText("");
        List<Map<String, Object>> subs = jdbc.queryForList("SELECT id, company_id, billing_cycle FROM subscriptions WHERE gateway_subscription_ref=?", subRef);
        if (subs.isEmpty()) return;
        long company = ((Number) subs.get(0).get("company_id")).longValue();
        BigDecimal total = BigDecimal.valueOf(inv.path("amount_paid").asLong(inv.path("amount_due").asLong())).movePointLeft(2);
        BigDecimal tax = BigDecimal.valueOf(inv.path("tax").asLong(0)).movePointLeft(2);
        String currency = inv.path("currency").asText("usd").toUpperCase();
        String number = inv.path("number").asText(inv.path("id").asText());
        try {
            jdbc.update("INSERT INTO platform_invoices(company_id,invoice_number,subtotal,tax,total,currency,status,paid_at) VALUES (?,?,?,?,?,?,?,?)",
                    company, number, total.subtract(tax), tax, total, currency, paid ? "PAID" : "FAILED", paid ? new java.sql.Timestamp(System.currentTimeMillis()) : null);
        } catch (DuplicateKeyException ignored) { /* invoice already recorded */ }
        Long invoiceId = jdbc.queryForObject("SELECT id FROM platform_invoices WHERE invoice_number=?", Long.class, number);
        jdbc.update("INSERT INTO platform_payments(company_id,platform_invoice_id,amount,currency,status,gateway,gateway_ref,failure_reason) VALUES (?,?,?,?,?,'stripe',?,?)",
                company, invoiceId, total, currency, paid ? "SUCCEEDED" : "FAILED", inv.path("payment_intent").asText(null), paid ? null : "Payment failed");
        if (paid) {
            String interval = "ANNUAL".equals(subs.get(0).get("billing_cycle")) ? "YEAR" : "MONTH";
            jdbc.update("UPDATE subscriptions SET status='ACTIVE', current_period_start=UTC_TIMESTAMP(), current_period_end=DATE_ADD(UTC_TIMESTAMP(), INTERVAL 1 " + interval + ") WHERE id=?", subs.get(0).get("id"));
            jdbc.update("UPDATE companies SET status='ACTIVE' WHERE id=? AND status IN ('TRIAL','EXPIRED')", company);
        } else {
            jdbc.update("UPDATE subscriptions SET status='PAST_DUE' WHERE id=?", subs.get(0).get("id"));
        }
    }

    private void deleted(JsonNode sub) {
        List<Map<String, Object>> subs = jdbc.queryForList("SELECT id, company_id FROM subscriptions WHERE gateway_subscription_ref=?", sub.path("id").asText(""));
        if (subs.isEmpty()) return;
        jdbc.update("UPDATE subscriptions SET status='CANCELLED', cancelled_at=UTC_TIMESTAMP() WHERE id=?", subs.get(0).get("id"));
        jdbc.update("UPDATE companies SET status='CANCELLED' WHERE id=? AND status='ACTIVE'", subs.get(0).get("company_id"));
    }
}
