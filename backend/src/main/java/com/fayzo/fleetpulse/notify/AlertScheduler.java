package com.fayzo.fleetpulse.notify;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Daily sweep that turns real data conditions into in-app notifications (user_id NULL = visible to the whole company)
 * and expires lapsed trials. Duplicates for the same entity are suppressed for 7 days.
 * Single-instance assumption: with several backend instances add a distributed lock (e.g. ShedLock) first.
 */
@Component
public class AlertScheduler {
    private static final Logger log = LoggerFactory.getLogger(AlertScheduler.class);
    private final JdbcTemplate jdbc;
    public AlertScheduler(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Scheduled(cron = "0 15 3 * * *", zone = "UTC")
    public void daily() {
        try {
            expireTrials();
            alert("LICENCE_EXPIRY", "WARNING", "Driver licence expiring", "drivers", "id", "CONCAT(full_name,' - licence expires ',licence_expiry)",
                    "licence_expiry IS NOT NULL AND licence_expiry <= DATE_ADD(CURDATE(), INTERVAL 30 DAY)");
            alert("VEHICLE_DOCUMENT_EXPIRY", "WARNING", "Vehicle document expiring", "vehicle_documents", "id", "CONCAT(doc_type,' expires ',expiry_date)",
                    "expiry_date IS NOT NULL AND expiry_date <= DATE_ADD(CURDATE(), INTERVAL 30 DAY)");
            alert("MAINTENANCE_DUE", "WARNING", "Maintenance due", "maintenance_records", "id", "CONCAT(title,' scheduled ',scheduled_on)",
                    "status='OPEN' AND scheduled_on IS NOT NULL AND scheduled_on <= DATE_ADD(CURDATE(), INTERVAL 7 DAY)");
            alert("PAYMENT_OVERDUE", "WARNING", "Invoice overdue", "invoices", "id", "CONCAT(invoice_number,' was due ',due_date)",
                    "kind='CUSTOMER' AND status NOT IN ('PAID','VOID','DRAFT') AND due_date IS NOT NULL AND due_date < CURDATE()");
            alert("SHIPMENT_DELAYED", "WARNING", "Shipment delayed", "shipments", "id", "CONCAT(tracking_id,' passed its scheduled delivery')",
                    "scheduled_delivery < UTC_TIMESTAMP() AND status NOT IN ('DELIVERED','CANCELLED','FAILED','RETURNED')");
        } catch (Exception e) {
            log.error("Daily alert sweep failed", e);
        }
    }

    private void alert(String type, String severity, String title, String table, String idCol, String messageExpr, String condition) {
        int n = jdbc.update("INSERT INTO notifications(company_id,type,severity,title,message,entity_type,entity_id) " +
                "SELECT t.company_id, ?, ?, ?, " + messageExpr + ", ?, t." + idCol + " FROM " + table + " t WHERE " + condition +
                " AND NOT EXISTS (SELECT 1 FROM notifications n WHERE n.company_id=t.company_id AND n.type=? AND n.entity_id=t." + idCol +
                " AND n.created_at > DATE_SUB(UTC_TIMESTAMP(), INTERVAL 7 DAY))", type, severity, title, table, type);
        if (n > 0) log.info("Created {} {} notifications", n, type);
    }

    private void expireTrials() {
        int n = jdbc.update("UPDATE subscriptions SET status='EXPIRED' WHERE status='TRIALING' AND trial_ends_at IS NOT NULL AND trial_ends_at < UTC_TIMESTAMP()");
        if (n > 0) {
            jdbc.update("UPDATE companies c SET c.status='EXPIRED' WHERE c.status='TRIAL' AND EXISTS (SELECT 1 FROM subscriptions s WHERE s.company_id=c.id AND s.status='EXPIRED') AND NOT EXISTS (SELECT 1 FROM subscriptions s2 WHERE s2.company_id=c.id AND s2.status IN ('TRIALING','ACTIVE'))");
            log.info("Expired {} trial subscriptions", n);
        }
    }
}
