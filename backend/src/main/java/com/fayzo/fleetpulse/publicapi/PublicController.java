package com.fayzo.fleetpulse.publicapi;

import com.fayzo.fleetpulse.common.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Unauthenticated endpoints of the public website (rate limited per IP): contact / demo requests and delivery feedback by tracking ID. */
@RestController
@RequestMapping("/api/v1/public")
public class PublicController {
    public record ContactRequest(@NotBlank @Pattern(regexp = "DEMO|CONTACT") String kind, @NotBlank @Size(max = 160) String name, @NotBlank @Email @Size(max = 190) String email,
                                 @Size(max = 24) String mobile, @Size(max = 160) String companyName, @Size(max = 4000) String message) {}
    public record FeedbackRequest(@NotNull @Min(1) @Max(5) Integer rating, @Size(max = 2000) String comment, @Pattern(regexp = "FEEDBACK|COMPLAINT") String kind) {}

    private final JdbcTemplate jdbc;
    public PublicController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @PostMapping("/contact")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Boolean> contact(@Valid @RequestBody ContactRequest r, HttpServletRequest http) {
        Integer recent = jdbc.queryForObject("SELECT COUNT(*) FROM contact_requests WHERE ip_address=? AND created_at > DATE_SUB(UTC_TIMESTAMP(), INTERVAL 1 HOUR)", Integer.class, http.getRemoteAddr());
        if (recent != null && recent >= 5) throw ApiException.tooMany("Too many requests from this address. Please try later.");
        jdbc.update("INSERT INTO contact_requests(kind,name,email,mobile,company_name,message,ip_address) VALUES (?,?,?,?,?,?,?)", r.kind(), r.name().trim(), r.email().trim().toLowerCase(), r.mobile(), r.companyName(), r.message(), http.getRemoteAddr());
        return Map.of("ok", true);
    }

    /** Only for delivered shipments, at most 3 submissions per shipment; the company is taken from the shipment, never from the request. */
    @PostMapping("/tracking/{trackingId}/feedback")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Boolean> feedback(@PathVariable String trackingId, @Valid @RequestBody FeedbackRequest r) {
        if (!trackingId.matches("^[A-Za-z0-9-]{6,30}$")) throw ApiException.notFound("Shipment not found");
        List<Map<String, Object>> s = jdbc.queryForList("SELECT id, company_id, customer_id, status FROM shipments WHERE tracking_id=?", trackingId.toUpperCase());
        if (s.isEmpty()) throw ApiException.notFound("Shipment not found");
        if (!"DELIVERED".equals(s.get(0).get("status"))) throw ApiException.badRequest("NOT_DELIVERED", "Feedback can be left once the shipment is delivered");
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM customer_feedback WHERE shipment_id=? AND company_id=?", Integer.class, s.get(0).get("id"), s.get(0).get("company_id"));
        if (n != null && n >= 3) throw ApiException.conflict("LIMIT", "Feedback limit reached for this shipment");
        jdbc.update("INSERT INTO customer_feedback(company_id,customer_id,shipment_id,kind,rating,comment,status) VALUES (?,?,?,?,?,?,'OPEN')", s.get(0).get("company_id"), s.get(0).get("customer_id"), s.get(0).get("id"), r.kind() == null ? "FEEDBACK" : r.kind(), r.rating(), r.comment());
        return Map.of("ok", true);
    }
}
