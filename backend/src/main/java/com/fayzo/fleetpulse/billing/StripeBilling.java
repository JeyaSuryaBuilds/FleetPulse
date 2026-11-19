package com.fayzo.fleetpulse.billing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fayzo.fleetpulse.common.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/** Stripe REST client + webhook signature verification (no SDK). Inactive until FP_STRIPE_SECRET_KEY / FP_STRIPE_WEBHOOK_SECRET are set. */
@Component
public class StripeBilling {
    private final String secretKey, webhookSecret;
    private final RestClient http = RestClient.builder().baseUrl("https://api.stripe.com").build();

    public StripeBilling(@Value("${fleetpulse.billing.stripe-secret-key:}") String secretKey, @Value("${fleetpulse.billing.stripe-webhook-secret:}") String webhookSecret) {
        this.secretKey = secretKey; this.webhookSecret = webhookSecret;
    }

    public boolean configured() { return !secretKey.isBlank(); }

    private void require() {
        if (!configured()) throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "PAYMENTS_NOT_CONFIGURED", "Online payments are not configured on this server");
    }

    /** Creates a subscription Checkout Session using the plan's backend price. Amounts are converted to minor units (x100: not valid for zero/three-decimal currencies). */
    public String createCheckoutUrl(long companyId, String email, String planCode, String planName, BigDecimal price, String currency, boolean annual, String successUrl, String cancelUrl) {
        require();
        var f = new LinkedMultiValueMap<String, String>();
        f.add("mode", "subscription");
        f.add("success_url", successUrl);
        f.add("cancel_url", cancelUrl);
        f.add("customer_email", email);
        f.add("client_reference_id", String.valueOf(companyId));
        f.add("metadata[company_id]", String.valueOf(companyId));
        f.add("metadata[plan_code]", planCode);
        f.add("metadata[billing_cycle]", annual ? "ANNUAL" : "MONTHLY");
        f.add("subscription_data[metadata][company_id]", String.valueOf(companyId));
        f.add("line_items[0][quantity]", "1");
        f.add("line_items[0][price_data][currency]", currency.toLowerCase());
        f.add("line_items[0][price_data][unit_amount]", String.valueOf(price.movePointRight(2).longValueExact()));
        f.add("line_items[0][price_data][recurring][interval]", annual ? "year" : "month");
        f.add("line_items[0][price_data][product_data][name]", "FleetPulse " + planName + (annual ? " (annual)" : " (monthly)"));
        try {
            JsonNode r = http.post().uri("/v1/checkout/sessions").header("Authorization", "Bearer " + secretKey)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(f).retrieve().body(JsonNode.class);
            return r.path("url").asText();
        } catch (Exception e) { throw new ApiException(HttpStatus.BAD_GATEWAY, "PAYMENT_PROVIDER", "Could not start checkout"); }
    }

    public void cancelAtPeriodEnd(String subscriptionRef) {
        require();
        var f = new LinkedMultiValueMap<String, String>();
        f.add("cancel_at_period_end", "true");
        try {
            http.post().uri("/v1/subscriptions/" + subscriptionRef).header("Authorization", "Bearer " + secretKey)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(f).retrieve().toBodilessEntity();
        } catch (Exception e) { throw new ApiException(HttpStatus.BAD_GATEWAY, "PAYMENT_PROVIDER", "Could not cancel the subscription with the payment provider"); }
    }

    /** Verifies the Stripe-Signature header (t=timestamp,v1=hmac) with a 5 minute tolerance and constant-time comparison. */
    public boolean verify(String payload, String header) {
        if (webhookSecret.isBlank() || header == null) return false;
        try {
            String t = null; java.util.List<String> sigs = new java.util.ArrayList<>();
            for (String part : header.split(",")) {
                String[] kv = part.trim().split("=", 2);
                if (kv.length == 2 && kv[0].equals("t")) t = kv[1]; else if (kv.length == 2 && kv[0].equals("v1")) sigs.add(kv[1]);
            }
            if (t == null || sigs.isEmpty() || Math.abs(System.currentTimeMillis() / 1000 - Long.parseLong(t)) > 300) return false;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String expected = HexFormat.of().formatHex(mac.doFinal((t + "." + payload).getBytes(StandardCharsets.UTF_8)));
            for (String s : sigs) if (MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), s.getBytes(StandardCharsets.UTF_8))) return true;
            return false;
        } catch (Exception e) { return false; }
    }
}
