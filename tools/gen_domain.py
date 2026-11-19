#!/usr/bin/env python3
"""Generates Flyway V2 SQL + JPA entity/repository/controller classes for the tenant-owned domain modules.
Edit MODULES, run `python3 tools/gen_domain.py` from the repo root, and commit the output."""
import re, os
ROOT = os.path.join(os.path.dirname(__file__), '..', 'backend', 'src', 'main')
JAVA = os.path.join(ROOT, 'java', 'com', 'fayzo', 'fleetpulse', 'domain')
SQLF = os.path.join(ROOT, 'resources', 'db', 'migration', 'V2__domain_modules.sql')
PKG = 'com.fayzo.fleetpulse.domain'

# (table, Class, path, readPerm, writePerm, [fields], [unique tuples], readOnly)
# field: name:type[!][=default]   types: s<n> t i l d<p>.<s> b date dt fk.<table>
M = []
def mod(table, cls, path, rp, wp, fields, unique=(), ro=False):
    M.append(dict(table=table, cls=cls, path=path, rp=rp, wp=wp, fields=fields.split(), unique=list(unique), ro=ro))

mod('branches','Branch','branches','DASHBOARD_VIEW','ORG_MANAGE','name:s120! code:s30 address:s300 city:s80 phone:s30 managerName:s120 active:b=true')
mod('departments','Department','departments','DASHBOARD_VIEW','ORG_MANAGE','name:s120! branchId:fk.branches description:s300')
mod('warehouses','Warehouse','warehouses','WAREHOUSE_READ','WAREHOUSE_WRITE','name:s120! code:s30 branchId:fk.branches address:s300 city:s80 capacity:i active:b=true')
mod('vehicle_categories','VehicleCategory','vehicle-categories','FLEET_READ','FLEET_WRITE','name:s80! description:s300 defaultFuelType:s20')
mod('drivers','Driver','drivers','DRIVER_READ','DRIVER_WRITE','fullName:s160! mobile:s24 email:s190 userId:fk.users licenceNumber:s40 licenceClass:s20 licenceExpiry:date status:s20!=AVAILABLE branchId:fk.branches dateOfBirth:date address:s300 joinedOn:date')
mod('vehicles','Vehicle','vehicles','FLEET_READ','FLEET_WRITE','registrationNumber:s30! categoryId:fk.vehicle_categories make:s60 model:s60 manufactureYear:i vin:s40 fuelType:s20 tankCapacityLitres:d8.2 loadCapacityKg:d10.2 status:s20!=AVAILABLE odometerKm:d12.1 branchId:fk.branches assignedDriverId:fk.drivers gpsDeviceId:s60 purchaseDate:date purchasePrice:d14.2 residualValue:d14.2 usefulLifeYears:i saleDate:date salePrice:d14.2',[('registrationNumber',)])
mod('vehicle_documents','VehicleDocument','vehicle-documents','FLEET_READ','FLEET_WRITE','vehicleId:fk.vehicles! docType:s40! docNumber:s80 provider:s120 issueDate:date expiryDate:date fileUrl:s500')
mod('vehicle_inspections','VehicleInspection','vehicle-inspections','FLEET_READ','FLEET_WRITE','vehicleId:fk.vehicles! inspectedOn:date! inspectorName:s120 result:s20 odometerKm:d12.1 notes:t')
mod('customers','Customer','customers','CUSTOMER_READ','CUSTOMER_WRITE','name:s160! email:s190 mobile:s24 companyName:s160 taxId:s60 status:s20!=ACTIVE notes:t')
mod('customer_addresses','CustomerAddress','customer-addresses','CUSTOMER_READ','CUSTOMER_WRITE','customerId:fk.customers! label:s60 line1:s200! city:s80 state:s80 postalCode:s20 country:s80 latitude:d10.7 longitude:d10.7')
mod('vendors','Vendor','vendors','VENDOR_READ','VENDOR_WRITE','name:s160! vendorType:s30! contactName:s120 email:s190 mobile:s24 address:s300 taxId:s60 active:b=true')
mod('journeys','Journey','journeys','JOURNEY_READ','JOURNEY_WRITE','journeyCode:s30! vehicleId:fk.vehicles driverId:fk.drivers startLocation:s300! startLat:d10.7 startLng:d10.7 destination:s300! destLat:d10.7 destLng:d10.7 plannedStart:dt plannedEnd:dt actualStart:dt actualEnd:dt plannedDistanceKm:d10.2 actualDistanceKm:d10.2 estimatedFuelLitres:d10.2 actualFuelLitres:d10.2 etaAt:dt status:s20!=PLANNED notes:t',[('journeyCode',)])
mod('journey_stops','JourneyStop','journey-stops','JOURNEY_READ','JOURNEY_WRITE','journeyId:fk.journeys! sequenceNo:i! location:s300! lat:d10.7 lng:d10.7 plannedArrival:dt actualArrival:dt status:s20!=PENDING')
mod('routes','RoutePlan','routes','ROUTE_READ','ROUTE_WRITE','name:s160! journeyId:fk.journeys originName:s300 destinationName:s300 mode:s20!=FASTEST distanceKm:d10.2 durationMinutes:i tollEstimate:d12.2 fuelEstimateLitres:d10.2 waypointsJson:t computedBy:s60')
mod('driver_expenses','DriverExpense','driver-expenses','DRIVER_READ','DRIVER_WRITE','driverId:fk.drivers! journeyId:fk.journeys expenseDate:date! category:s40 amount:d12.2! description:s300')
mod('driver_accidents','DriverAccident','driver-accidents','DRIVER_READ','DRIVER_WRITE','driverId:fk.drivers! vehicleId:fk.vehicles occurredOn:date! location:s300 severity:s20 costEstimate:d12.2 description:t')
mod('shipments','Shipment','shipments','SHIPMENT_READ','SHIPMENT_WRITE','trackingId:s30! customerId:fk.customers journeyId:fk.journeys vehicleId:fk.vehicles driverId:fk.drivers warehouseId:fk.warehouses pickupAddress:s300! pickupLat:d10.7 pickupLng:d10.7 destinationAddress:s300! destLat:d10.7 destLng:d10.7 receiverName:s160 receiverMobile:s24 packageDescription:s300 weightKg:d10.2 volumeCbm:d10.3 packageCount:i declaredValue:d14.2 price:d14.2 scheduledPickup:dt scheduledDelivery:dt deliveredAt:dt status:s30!=CREATED failureReason:s300 returnShipment:b=false podUrl:s500 podSignedBy:s120 notes:t',[('trackingId',)])
mod('shipment_events','ShipmentEvent','shipment-events','SHIPMENT_READ','SHIPMENT_WRITE','shipmentId:fk.shipments! status:s30! note:s300 lat:d10.7 lng:d10.7 occurredAt:dt! publicVisible:b=true')
mod('geofences','Geofence','geofences','GPS_READ','ROUTE_WRITE','name:s120! centerLat:d10.7! centerLng:d10.7! radiusMeters:i! active:b=true')
mod('fuel_records','FuelRecord','fuel-records','FUEL_READ','FUEL_WRITE','vehicleId:fk.vehicles! driverId:fk.drivers journeyId:fk.journeys vendorId:fk.vendors fuelDate:dt! litres:d10.2! pricePerLitre:d10.4 totalCost:d12.2! odometerKm:d12.1 station:s120 receiptUrl:s500')
mod('fuel_prices','FuelPrice','fuel-prices','FUEL_READ','FUEL_WRITE','fuelType:s20! pricePerLitre:d10.4! effectiveFrom:date! region:s80')
mod('maintenance_records','MaintenanceRecord','maintenance','MAINTENANCE_READ','MAINTENANCE_WRITE','vehicleId:fk.vehicles! kind:s30! title:s160! description:t status:s20!=OPEN scheduledOn:date completedOn:date odometerKm:d12.1 serviceCentreId:fk.vendors cost:d12.2 nextDueDate:date nextDueKm:d12.1')
mod('spare_parts','SparePart','spare-parts','MAINTENANCE_READ','MAINTENANCE_WRITE','name:s160! sku:s60 vendorId:fk.vendors quantity:i=0 unitCost:d12.2 reorderLevel:i')
mod('purchase_orders','PurchaseOrder','purchase-orders','VENDOR_READ','VENDOR_WRITE','poNumber:s30! vendorId:fk.vendors! orderDate:date! status:s20!=DRAFT totalAmount:d14.2 notes:t',[('poNumber',)])
mod('shifts','Shift','shifts','EMPLOYEE_READ','EMPLOYEE_WRITE','name:s80! startTime:s8! endTime:s8!')
mod('employees','Employee','employees','EMPLOYEE_READ','EMPLOYEE_WRITE','employeeCode:s30! fullName:s160! email:s190 mobile:s24 userId:fk.users driverId:fk.drivers departmentId:fk.departments branchId:fk.branches shiftId:fk.shifts jobTitle:s120 status:s20!=ACTIVE joinedOn:date',[('employeeCode',)])
mod('attendance','Attendance','attendance','EMPLOYEE_READ','EMPLOYEE_WRITE','employeeId:fk.employees! workDate:date! checkIn:dt checkOut:dt status:s20!=PRESENT lat:d10.7 lng:d10.7 notes:s300',[('employeeId','workDate')])
mod('leave_requests','LeaveRequest','leave-requests','EMPLOYEE_READ','EMPLOYEE_WRITE','employeeId:fk.employees! leaveType:s30! fromDate:date! toDate:date! status:s20!=PENDING reason:s300 decidedBy:l')
mod('tasks','WorkTask','tasks','EMPLOYEE_READ','EMPLOYEE_WRITE','title:s160! description:t assignedEmployeeId:fk.employees dueDate:date status:s20!=OPEN priority:s10')
mod('salary_structures','SalaryStructure','salary-structures','PAYROLL_READ','PAYROLL_WRITE','employeeId:fk.employees! model:s20!=FIXED monthlySalary:d12.2 overtimeRate:d12.2 perDeliveryRate:d12.2 perTripRate:d12.2 perKmRate:d12.4 perTaskRate:d12.2 effectiveFrom:date',[('employeeId',)])
mod('work_records','WorkRecord','work-records','PAYROLL_READ','PAYROLL_WRITE','employeeId:fk.employees! workType:s20! referenceId:l quantity:d10.2! rate:d12.2 amount:d12.2 workDate:date! status:s20!=PENDING approvedBy:l')
mod('salary_advances','SalaryAdvance','salary-advances','PAYROLL_READ','PAYROLL_WRITE','employeeId:fk.employees! amount:d12.2! advanceDate:date! repaid:d12.2=0 status:s20!=OPEN')
mod('payroll_runs','PayrollRun','payroll-runs','PAYROLL_READ','PAYROLL_WRITE','periodMonth:s7! status:s20!=DRAFT totalNet:d14.2',[('periodMonth',)])
mod('payslips','Payslip','payslips','PAYROLL_READ','PAYROLL_WRITE','payrollRunId:fk.payroll_runs employeeId:fk.employees! periodMonth:s7! model:s20 basic:d12.2 overtime:d12.2 bonus:d12.2 deductions:d12.2 advances:d12.2 workEarnings:d12.2 netSalary:d12.2! workingDays:i presentDays:i leaveDays:i status:s20!=DRAFT paidOn:date',[('employeeId','periodMonth')])
mod('inventory_items','InventoryItem','inventory-items','WAREHOUSE_READ','WAREHOUSE_WRITE','warehouseId:fk.warehouses! sku:s60! name:s160! barcode:s80 quantity:i=0 unit:s20 reorderLevel:i',[('warehouseId','sku')])
mod('stock_transfers','StockTransfer','stock-transfers','WAREHOUSE_READ','WAREHOUSE_WRITE','fromWarehouseId:fk.warehouses! toWarehouseId:fk.warehouses! itemId:fk.inventory_items! quantity:i! status:s20!=COMPLETED transferredAt:dt')
mod('inventory_history','InventoryHistory','inventory-history','WAREHOUSE_READ','WAREHOUSE_WRITE','itemId:fk.inventory_items! changeQty:i! reason:s60 refType:s40 refId:l balanceAfter:i occurredAt:dt!',ro=False)
mod('invoices','Invoice','invoices','FINANCE_READ','FINANCE_WRITE','invoiceNumber:s30! kind:s10! customerId:fk.customers vendorId:fk.vendors shipmentId:fk.shipments issueDate:date! dueDate:date subtotal:d14.2 tax:d14.2 total:d14.2! status:s20!=DRAFT currency:s3 notes:t',[('invoiceNumber',)])
mod('payments','Payment','payments','FINANCE_READ','FINANCE_WRITE','invoiceId:fk.invoices direction:s10! amount:d14.2! method:s30 reference:s120 paidAt:dt! gatewayRef:s120')
mod('expenses','Expense','expenses','FINANCE_READ','FINANCE_WRITE','category:s40! amount:d14.2! expenseDate:date! vehicleId:fk.vehicles vendorId:fk.vendors description:s300')
mod('notifications','Notification','notifications','NOTIFICATION_READ','ORG_MANAGE','userId:fk.users type:s40! severity:s10!=INFO title:s200! message:t entityType:s40 entityId:l readAt:dt')
mod('support_tickets','SupportTicket','support-tickets','SUPPORT_MANAGE','SUPPORT_MANAGE','category:s20!=SUPPORT subject:s200! description:t status:s20!=OPEN priority:s10!=NORMAL createdByUserId:fk.users')
mod('customer_feedback','CustomerFeedback','customer-feedback','CUSTOMER_READ','CUSTOMER_WRITE','customerId:fk.customers shipmentId:fk.shipments kind:s20!=FEEDBACK rating:i comment:t status:s20!=OPEN')

def snake(n): return re.sub(r'([A-Z])', lambda m: '_' + m.group(1).lower(), n)
def parse(f):
    name, rest = f.split(':', 1)
    default = None
    if '=' in rest: rest, default = rest.split('=', 1)
    req = rest.endswith('!')
    t = rest.rstrip('!')
    return name, t, req, default

def isdec(t): return re.match(r'^d\d+\.\d+$', t) is not None

def sqltype(t):
    if t.startswith('s'): return f'VARCHAR({t[1:]})'
    if t == 't': return 'TEXT'
    if t == 'i': return 'INT'
    if t == 'l': return 'BIGINT'
    if isdec(t): p, s = t[1:].split('.'); return f'DECIMAL({p},{s})'
    if t == 'b': return 'BOOLEAN'
    if t == 'date': return 'DATE'
    if t == 'dt': return 'DATETIME(3)'
    if t.startswith('fk.'): return 'BIGINT'
    raise ValueError(t)

def jtype(t):
    if t.startswith('s') or t == 't': return 'String'
    if t == 'i': return 'Integer'
    if t == 'l' or t.startswith('fk.'): return 'Long'
    if isdec(t): return 'BigDecimal'
    if t == 'b': return 'Boolean'
    if t == 'date': return 'LocalDate'
    if t == 'dt': return 'Instant'

sql = ['-- FleetPulse V2: tenant-owned domain tables (generated by tools/gen_domain.py; edit the generator, not this file after release)\n']
for m in M:
    cols = ['  id BIGINT AUTO_INCREMENT PRIMARY KEY', '  company_id BIGINT NOT NULL']
    cons, idx = [], [f'  INDEX ix_{m["table"]}_company (company_id)']
    for f in m['fields']:
        n, t, req, df = parse(f)
        c = f'  {snake(n)} {sqltype(t)}' + (' NOT NULL' if req else '')
        if df is not None:
            c += ' DEFAULT ' + (df.upper() if t == 'b' else df if (t in ('i','l') or isdec(t)) else f"'{df}'")
        cols.append(c)
        if t.startswith('fk.'):
            tgt = t[3:]
            cons.append(f'  CONSTRAINT fk_{m["table"]}_{snake(n)} FOREIGN KEY ({snake(n)}) REFERENCES {tgt}(id)')
            idx.append(f'  INDEX ix_{m["table"]}_{snake(n)} ({snake(n)})')
    cols += ['  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP', '  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP']
    cons.append(f'  CONSTRAINT fk_{m["table"]}_company FOREIGN KEY (company_id) REFERENCES companies(id)')
    for u in m['unique']:
        cons.append(f'  CONSTRAINT uq_{m["table"]}_{"_".join(snake(x) for x in u)} UNIQUE (company_id, {", ".join(snake(x) for x in u)})')
    sql.append(f'CREATE TABLE {m["table"]} (\n' + ',\n'.join(cols + cons + idx) + '\n) ENGINE=InnoDB;\n')

sql.append('''
-- High-volume GPS positions (written only by authenticated driver apps / registered GPS devices; never simulated)
CREATE TABLE gps_locations (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  company_id BIGINT NOT NULL,
  vehicle_id BIGINT NULL,
  driver_id BIGINT NULL,
  journey_id BIGINT NULL,
  latitude DECIMAL(10,7) NOT NULL,
  longitude DECIMAL(10,7) NOT NULL,
  speed_kmh DECIMAL(6,2) NULL,
  heading_deg DECIMAL(5,1) NULL,
  accuracy_m DECIMAL(8,2) NULL,
  source VARCHAR(20) NOT NULL,           -- DRIVER_APP or GPS_DEVICE
  recorded_at DATETIME(3) NOT NULL,
  received_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  CONSTRAINT fk_gps_company FOREIGN KEY (company_id) REFERENCES companies(id),
  CONSTRAINT fk_gps_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id),
  CONSTRAINT fk_gps_driver FOREIGN KEY (driver_id) REFERENCES drivers(id),
  INDEX ix_gps_vehicle_time (company_id, vehicle_id, recorded_at),
  INDEX ix_gps_journey (journey_id, recorded_at)
) ENGINE=InnoDB;

CREATE TABLE ai_usage (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  company_id BIGINT NOT NULL,
  user_id BIGINT NULL,
  feature VARCHAR(40) NOT NULL,
  model VARCHAR(120) NOT NULL,
  prompt_tokens INT NOT NULL DEFAULT 0,
  completion_tokens INT NOT NULL DEFAULT 0,
  success BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_ai_company FOREIGN KEY (company_id) REFERENCES companies(id),
  INDEX ix_ai_company_time (company_id, created_at)
) ENGINE=InnoDB;
''')
os.makedirs(os.path.dirname(SQLF), exist_ok=True)
open(SQLF, 'w').write('\n'.join(sql))

os.makedirs(JAVA, exist_ok=True)
for m in M:
    cls = m['cls']
    imports = {'jakarta.persistence.*', 'jakarta.validation.constraints.*'}
    body, fks = [], {}
    for f in m['fields']:
        n, t, req, df = parse(f)
        jt = jtype(t)
        if jt == 'BigDecimal': imports.add('java.math.BigDecimal')
        if jt == 'LocalDate': imports.add('java.time.LocalDate')
        if jt == 'Instant': imports.add('java.time.Instant')
        ann = []
        if req and jt == 'String': ann.append('@NotBlank')
        elif req: ann.append('@NotNull')
        if jt == 'String' and t.startswith('s'): ann.append(f'@Size(max = {t[1:]})')
        if jt == 'BigDecimal':
            p, s = t[1:].split('.'); ann.append(f'@Digits(integer = {int(p)-int(s)}, fraction = {s})')
        if t.startswith('fk.'): fks[n] = t[3:]
        init = ''
        if df is not None:
            init = ' = ' + ({'b': df, 'i': df, 'l': df + 'L'}.get(t) if t in ('b','i','l') else
                            f'new BigDecimal("{df}")' if jt == 'BigDecimal' else f'"{df}"')
        body.append('    ' + ' '.join(ann) + f'\n    @Column(name = "{snake(n)}")\n    private {jt} {n}{init};\n')
    open(f'{JAVA}/{cls}.java', 'w').write(
        f'package {PKG};\n\n' + '\n'.join(f'import {i};' for i in sorted(imports)) +
        f'\n\n@Entity\n@Table(name = "{m["table"]}")\npublic class {cls} extends BaseEntity {{\n' + '\n'.join(body) + '}\n')
    open(f'{JAVA}/{cls}Repository.java', 'w').write(
        f'package {PKG};\n\nimport org.springframework.data.jpa.repository.JpaRepository;\nimport org.springframework.data.jpa.repository.JpaSpecificationExecutor;\n\n'
        f'public interface {cls}Repository extends JpaRepository<{cls}, Long>, JpaSpecificationExecutor<{cls}> {{}}\n')
    fkmap = 'Map.of(' + ', '.join(f'"{k}", "{v}"' for k, v in fks.items()) + ')' if fks else 'Map.of()'
    open(f'{JAVA}/{cls}Controller.java', 'w').write(
        f'package {PKG};\n\nimport com.fayzo.fleetpulse.security.Permission;\nimport org.springframework.web.bind.annotation.*;\n\nimport java.util.Map;\n\n'
        f'@RestController\n@RequestMapping("/api/v1/{m["path"]}")\n'
        f'public class {cls}Controller extends CrudController<{cls}, {cls}Repository> {{\n'
        f'    public {cls}Controller({cls}Repository repo, CrudDeps deps) {{\n'
        f'        super({cls}.class, repo, deps, Permission.{m["rp"]}, Permission.{m["wp"]},\n'
        f'                {fkmap}, "{cls}");\n    }}\n}}\n')

# ---- post-generation patches ----
def patch(path, old, new):
    t = open(path).read()
    assert old in t, (path, old)
    open(path, 'w').write(t.replace(old, new, 1))
patch(f'{JAVA}/Shipment.java', 'private String trackingId;', 'private String trackingId = com.fayzo.fleetpulse.common.IdGen.trackingId();')
patch(f'{JAVA}/Journey.java', 'private String journeyCode;', 'private String journeyCode = com.fayzo.fleetpulse.common.IdGen.code("JR");')
patch(f'{JAVA}/PurchaseOrder.java', 'private String poNumber;', 'private String poNumber = com.fayzo.fleetpulse.common.IdGen.code("PO");')
patch(f'{JAVA}/Invoice.java', 'private String invoiceNumber;', 'private String invoiceNumber = com.fayzo.fleetpulse.common.IdGen.code("INV");')
patch(f'{JAVA}/Employee.java', 'private String employeeCode;', 'private String employeeCode = com.fayzo.fleetpulse.common.IdGen.code("EMP");')
open(f'{JAVA}/VehicleController.java', 'w').write(f'''package {PKG};

import com.fayzo.fleetpulse.security.Permission;
import com.fayzo.fleetpulse.subscription.PlanLimits;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/vehicles")
public class VehicleController extends CrudController<Vehicle, VehicleRepository> {{
    private final PlanLimits limits;

    public VehicleController(VehicleRepository repo, CrudDeps deps, PlanLimits limits) {{
        super(Vehicle.class, repo, deps, Permission.FLEET_READ, Permission.FLEET_WRITE,
                Map.of("categoryId", "vehicle_categories", "branchId", "branches", "assignedDriverId", "drivers"), "Vehicle");
        this.limits = limits;
    }}

    @Override
    protected void beforeCreate(Vehicle v, com.fasterxml.jackson.databind.JsonNode body) {{ limits.requireVehicleCapacity(); }}
}}
''')
open(f'{JAVA}/ShipmentController.java', 'w').write(f"""package {PKG};

import com.fasterxml.jackson.databind.JsonNode;
import com.fayzo.fleetpulse.common.ApiException;
import com.fayzo.fleetpulse.security.Permission;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/shipments")
public class ShipmentController extends CrudController<Shipment, ShipmentRepository> {{
    public ShipmentController(ShipmentRepository repo, CrudDeps deps) {{
        super(Shipment.class, repo, deps, Permission.SHIPMENT_READ, Permission.SHIPMENT_WRITE,
                Map.of("customerId", "customers", "journeyId", "journeys", "vehicleId", "vehicles", "driverId", "drivers", "warehouseId", "warehouses"), "Shipment");
    }}

    @Override
    protected void beforeCreate(Shipment s, JsonNode body) {{
        String st = body.path("status").asText("CREATED");
        if (!st.equals("CREATED") && !st.equals("ASSIGNED")) throw ApiException.badRequest("BAD_STATUS", "New shipments start as CREATED or ASSIGNED");
        if (body.has("deliveredAt") || body.has("podUrl") || body.has("podSignedBy"))
            throw ApiException.badRequest("USE_WORKFLOW", "Delivery fields are set through POST /shipments/{{id}}/status");
    }}

    @Override
    protected void beforeUpdate(Shipment existing, JsonNode body) {{
        for (String f : new String[]{{"status", "deliveredAt", "podUrl", "podSignedBy", "failureReason", "trackingId"}})
            if (body.has(f)) throw ApiException.badRequest("USE_WORKFLOW", f + " cannot be edited here. Use POST /api/v1/shipments/{{id}}/status");
    }}
}}
""")
print(len(M), 'modules generated')
