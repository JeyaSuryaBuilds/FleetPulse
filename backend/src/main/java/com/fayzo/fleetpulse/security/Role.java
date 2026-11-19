package com.fayzo.fleetpulse.security;

import java.util.EnumSet;
import java.util.Set;

import static com.fayzo.fleetpulse.security.Permission.*;

/** Backend-enforced role to permission map. Mirrors the seeded roles table. */
public enum Role {
    COMPANY_ADMIN(EnumSet.allOf(Permission.class)),
    OPERATIONS_MANAGER(EnumSet.of(DASHBOARD_VIEW, FLEET_READ, DRIVER_READ, JOURNEY_READ, JOURNEY_WRITE, SHIPMENT_READ, SHIPMENT_WRITE,
            GPS_READ, ROUTE_READ, ROUTE_WRITE, CUSTOMER_READ, CUSTOMER_WRITE, WAREHOUSE_READ, REPORT_VIEW, AI_USE, NOTIFICATION_READ)),
    FLEET_MANAGER(EnumSet.of(DASHBOARD_VIEW, FLEET_READ, FLEET_WRITE, DRIVER_READ, DRIVER_WRITE, JOURNEY_READ, GPS_READ,
            FUEL_READ, FUEL_WRITE, MAINTENANCE_READ, MAINTENANCE_WRITE, VENDOR_READ, REPORT_VIEW, AI_USE, NOTIFICATION_READ)),
    ACCOUNTANT(EnumSet.of(DASHBOARD_VIEW, FINANCE_READ, FINANCE_WRITE, PAYROLL_READ, PAYROLL_WRITE, VENDOR_READ, VENDOR_WRITE,
            CUSTOMER_READ, REPORT_VIEW, AI_USE, NOTIFICATION_READ)),
    HR_MANAGER(EnumSet.of(DASHBOARD_VIEW, EMPLOYEE_READ, EMPLOYEE_WRITE, DRIVER_READ, PAYROLL_READ, PAYROLL_WRITE, REPORT_VIEW, NOTIFICATION_READ)),
    DRIVER(EnumSet.of(SELF_SERVICE, JOURNEY_SELF, SHIPMENT_SELF, GPS_PUBLISH, FUEL_SELF, ATTENDANCE_SELF, NOTIFICATION_READ)),
    WAREHOUSE_MANAGER(EnumSet.of(DASHBOARD_VIEW, WAREHOUSE_READ, WAREHOUSE_WRITE, SHIPMENT_READ, NOTIFICATION_READ)),
    CUSTOMER_SUPPORT(EnumSet.of(DASHBOARD_VIEW, CUSTOMER_READ, CUSTOMER_WRITE, SHIPMENT_READ, SUPPORT_MANAGE, NOTIFICATION_READ));

    private final Set<Permission> permissions;
    Role(Set<Permission> p) {
        // every staff member may view their own payslips/earnings and record their own attendance
        EnumSet<Permission> all = EnumSet.copyOf(p);
        all.add(SELF_SERVICE);
        all.add(ATTENDANCE_SELF);
        this.permissions = all;
    }
    public Set<Permission> permissions() { return permissions; }
}
