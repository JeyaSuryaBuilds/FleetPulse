package com.fayzo.fleetpulse.report;

import com.fayzo.fleetpulse.security.Permission;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fixed, parameterised report queries. Placeholders: :c = company id (always bound server-side from the token),
 * :from / :to = date range (to is exclusive after +1 day). No user text is ever concatenated into SQL.
 */
public final class ReportDefinitions {
    public record Def(String title, Permission permission, String sql) {}
    public static final Map<String, Def> ALL = new LinkedHashMap<>();
    private ReportDefinitions() {}

    static {
        ALL.put("fleet", new Def("Fleet Report", Permission.FLEET_READ, """
            SELECT registration_number AS registration, make, model, status, fuel_type AS fuel_type, odometer_km AS odometer_km,
                   purchase_date AS purchase_date, purchase_price AS purchase_price
            FROM vehicles WHERE company_id=:c ORDER BY registration_number"""));
        ALL.put("vehicle", new Def("Vehicle Cost Report", Permission.FLEET_READ, """
            SELECT v.registration_number AS registration,
              COALESCE((SELECT SUM(f.litres) FROM fuel_records f WHERE f.vehicle_id=v.id AND f.company_id=:c AND f.fuel_date>=:from AND f.fuel_date<:to),0) AS fuel_litres,
              COALESCE((SELECT SUM(f.total_cost) FROM fuel_records f WHERE f.vehicle_id=v.id AND f.company_id=:c AND f.fuel_date>=:from AND f.fuel_date<:to),0) AS fuel_cost,
              COALESCE((SELECT SUM(m.cost) FROM maintenance_records m WHERE m.vehicle_id=v.id AND m.company_id=:c AND COALESCE(m.completed_on,m.scheduled_on)>=:from AND COALESCE(m.completed_on,m.scheduled_on)<:to),0) AS maintenance_cost,
              COALESCE((SELECT SUM(j.actual_distance_km) FROM journeys j WHERE j.vehicle_id=v.id AND j.company_id=:c AND j.actual_end>=:from AND j.actual_end<:to),0) AS distance_km
            FROM vehicles v WHERE v.company_id=:c ORDER BY v.registration_number"""));
        ALL.put("driver", new Def("Driver Report", Permission.DRIVER_READ, """
            SELECT d.full_name AS driver, d.status, d.licence_expiry AS licence_expiry,
              (SELECT COUNT(*) FROM shipments s WHERE s.driver_id=d.id AND s.company_id=:c AND s.status='DELIVERED' AND s.delivered_at>=:from AND s.delivered_at<:to) AS deliveries,
              (SELECT COUNT(*) FROM shipments s WHERE s.driver_id=d.id AND s.company_id=:c AND s.status='FAILED' AND s.created_at>=:from AND s.created_at<:to) AS failed_deliveries,
              (SELECT COUNT(*) FROM journeys j WHERE j.driver_id=d.id AND j.company_id=:c AND j.status='COMPLETED' AND j.actual_end>=:from AND j.actual_end<:to) AS journeys_completed,
              (SELECT COUNT(*) FROM driver_accidents a WHERE a.driver_id=d.id AND a.company_id=:c AND a.occurred_on>=:from AND a.occurred_on<:to) AS accidents
            FROM drivers d WHERE d.company_id=:c ORDER BY d.full_name"""));
        ALL.put("shipment", new Def("Shipment Report", Permission.SHIPMENT_READ, """
            SELECT s.tracking_id AS tracking_id, c.name AS customer, s.status, s.pickup_address AS pickup, s.destination_address AS destination,
                   s.scheduled_delivery AS scheduled_delivery, s.delivered_at AS delivered_at, s.price
            FROM shipments s LEFT JOIN customers c ON c.id=s.customer_id AND c.company_id=s.company_id
            WHERE s.company_id=:c AND s.created_at>=:from AND s.created_at<:to ORDER BY s.id DESC"""));
        ALL.put("fuel", new Def("Fuel Report", Permission.FUEL_READ, """
            SELECT v.registration_number AS vehicle, f.fuel_date AS fuel_date, f.litres, f.price_per_litre AS price_per_litre, f.total_cost AS total_cost,
                   f.odometer_km AS odometer_km, f.station
            FROM fuel_records f JOIN vehicles v ON v.id=f.vehicle_id AND v.company_id=f.company_id
            WHERE f.company_id=:c AND f.fuel_date>=:from AND f.fuel_date<:to ORDER BY f.fuel_date DESC"""));
        ALL.put("maintenance", new Def("Maintenance Report", Permission.MAINTENANCE_READ, """
            SELECT v.registration_number AS vehicle, m.kind, m.title, m.status, m.scheduled_on AS scheduled_on, m.completed_on AS completed_on, m.cost
            FROM maintenance_records m JOIN vehicles v ON v.id=m.vehicle_id AND v.company_id=m.company_id
            WHERE m.company_id=:c AND COALESCE(m.completed_on,m.scheduled_on)>=:from AND COALESCE(m.completed_on,m.scheduled_on)<:to ORDER BY m.id DESC"""));
        ALL.put("employee", new Def("Employee Report", Permission.EMPLOYEE_READ, """
            SELECT employee_code AS code, full_name AS name, job_title AS job_title, status, joined_on AS joined_on
            FROM employees WHERE company_id=:c ORDER BY full_name"""));
        ALL.put("payroll", new Def("Payroll Report", Permission.PAYROLL_READ, """
            SELECT p.period_month AS period, e.employee_code AS code, e.full_name AS employee, p.model, p.basic, p.overtime, p.bonus, p.deductions,
                   p.advances, p.work_earnings AS work_earnings, p.net_salary AS net_salary, p.status
            FROM payslips p JOIN employees e ON e.id=p.employee_id AND e.company_id=p.company_id
            WHERE p.company_id=:c AND p.period_month>=DATE_FORMAT(:from,'%Y-%m') AND p.period_month<=DATE_FORMAT(DATE_SUB(:to, INTERVAL 1 DAY),'%Y-%m') ORDER BY p.period_month DESC, e.full_name"""));
        ALL.put("financial", new Def("Financial Report (Profit and Loss)", Permission.FINANCE_READ, """
            SELECT m.month, COALESCE(r.rev,0) AS revenue, COALESCE(e.exp,0) AS expenses, COALESCE(r.rev,0)-COALESCE(e.exp,0) AS profit
            FROM (SELECT DATE_FORMAT(paid_at,'%Y-%m') AS month FROM payments WHERE company_id=:c AND direction='IN' AND paid_at>=:from AND paid_at<:to
                  UNION SELECT DATE_FORMAT(expense_date,'%Y-%m') FROM expenses WHERE company_id=:c AND expense_date>=:from AND expense_date<:to) m
            LEFT JOIN (SELECT DATE_FORMAT(paid_at,'%Y-%m') AS month, SUM(amount) AS rev FROM payments WHERE company_id=:c AND direction='IN' AND paid_at>=:from AND paid_at<:to GROUP BY 1) r ON r.month=m.month
            LEFT JOIN (SELECT DATE_FORMAT(expense_date,'%Y-%m') AS month, SUM(amount) AS exp FROM expenses WHERE company_id=:c AND expense_date>=:from AND expense_date<:to GROUP BY 1) e ON e.month=m.month
            ORDER BY m.month"""));
        ALL.put("customer", new Def("Customer Report", Permission.CUSTOMER_READ, """
            SELECT c.name AS customer, c.status,
              (SELECT COUNT(*) FROM shipments s WHERE s.customer_id=c.id AND s.company_id=:c AND s.created_at>=:from AND s.created_at<:to) AS shipments,
              (SELECT COALESCE(SUM(i.total),0) FROM invoices i WHERE i.customer_id=c.id AND i.company_id=:c AND i.kind='CUSTOMER' AND i.status<>'VOID' AND i.issue_date>=:from AND i.issue_date<:to) AS invoiced,
              (SELECT COALESCE(SUM(p.amount),0) FROM payments p JOIN invoices i2 ON i2.id=p.invoice_id AND i2.company_id=p.company_id WHERE i2.customer_id=c.id AND p.company_id=:c AND p.direction='IN' AND p.paid_at>=:from AND p.paid_at<:to) AS paid
            FROM customers c WHERE c.company_id=:c ORDER BY c.name"""));
    }
}
