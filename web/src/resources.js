// Declarative config for every standard tenant module. The backend (CrudController) enforces permissions and tenant isolation;
// these configs only drive the screens. Field keys match the API's camelCase JSON.
const S = (key, label, req = false, extra = {}) => ({ key, label, type: 'text', req, ...extra });
const TA = (key, label, req = false) => ({ key, label, type: 'textarea', req });
const N = (key, label, req = false, extra = {}) => ({ key, label, type: 'number', req, step: 'any', ...extra });
const M = (key, label, req = false) => ({ key, label, type: 'number', req, step: '0.01', money: true });
const D = (key, label, req = false) => ({ key, label, type: 'date', req });
const DT = (key, label, req = false) => ({ key, label, type: 'datetime', req });
const B = (key, label) => ({ key, label, type: 'bool' });
const SEL = (key, label, options, req = false) => ({ key, label, type: 'select', options, req });
const REF = (key, label, path, labelKey, req = false) => ({ key, label, type: 'ref', ref: { path, labelKey }, req });

const VEHICLE = REF('vehicleId', 'Vehicle', 'vehicles', 'registrationNumber');
const DRIVER = REF('driverId', 'Driver', 'drivers', 'fullName');
const EMP = REF('employeeId', 'Employee', 'employees', 'fullName');
const CUST = REF('customerId', 'Customer', 'customers', 'name');
const VENDOR = REF('vendorId', 'Vendor', 'vendors', 'name');
const BRANCH = REF('branchId', 'Branch', 'branches', 'name');
const WH = REF('warehouseId', 'Warehouse', 'warehouses', 'name');
const req = (f) => ({ ...f, req: true });
const noWrite = (f) => ({ ...f, readOnly: true });

export const resources = [
  { group: 'Organization', path: 'branches', title: 'Branches', read: 'DASHBOARD_VIEW', write: 'ORG_MANAGE', cols: ['name', 'code', 'city', 'phone', 'active'],
    fields: [S('name', 'Name', true), S('code', 'Code'), S('address', 'Address'), S('city', 'City'), S('phone', 'Phone'), S('managerName', 'Manager'), B('active', 'Active')] },
  { group: 'Organization', path: 'departments', title: 'Departments', read: 'DASHBOARD_VIEW', write: 'ORG_MANAGE', cols: ['name', 'branchId', 'description'],
    fields: [S('name', 'Name', true), BRANCH, TA('description', 'Description')] },
  { group: 'Organization', path: 'warehouses', title: 'Warehouses', read: 'WAREHOUSE_READ', write: 'WAREHOUSE_WRITE', cols: ['name', 'code', 'city', 'capacity', 'active'],
    fields: [S('name', 'Name', true), S('code', 'Code'), BRANCH, S('address', 'Address'), S('city', 'City'), N('capacity', 'Capacity'), B('active', 'Active')] },

  { group: 'Fleet', path: 'vehicles', title: 'Vehicles', read: 'FLEET_READ', write: 'FLEET_WRITE', statusOptions: ['AVAILABLE', 'IN_USE', 'MAINTENANCE', 'RETIRED', 'SOLD'],
    cols: ['registrationNumber', 'make', 'model', 'status', 'odometerKm', 'assignedDriverId'],
    fields: [S('registrationNumber', 'Registration number', true), REF('categoryId', 'Category', 'vehicle-categories', 'name'), S('make', 'Make'), S('model', 'Model'), N('manufactureYear', 'Year'), S('vin', 'VIN'),
      SEL('fuelType', 'Fuel type', ['DIESEL', 'PETROL', 'CNG', 'LPG', 'ELECTRIC', 'HYBRID']), N('tankCapacityLitres', 'Tank capacity (L)'), N('loadCapacityKg', 'Load capacity (kg)'),
      SEL('status', 'Status', ['AVAILABLE', 'IN_USE', 'MAINTENANCE', 'RETIRED', 'SOLD'], true), N('odometerKm', 'Odometer (km)'), BRANCH, REF('assignedDriverId', 'Assigned driver', 'drivers', 'fullName'),
      S('gpsDeviceId', 'GPS device ID'), D('purchaseDate', 'Purchase date'), M('purchasePrice', 'Purchase price'), M('residualValue', 'Residual value'), N('usefulLifeYears', 'Useful life (years)'),
      D('saleDate', 'Sale date'), M('salePrice', 'Sale price')] },
  { group: 'Fleet', path: 'vehicle-categories', title: 'Vehicle categories', read: 'FLEET_READ', write: 'FLEET_WRITE', cols: ['name', 'defaultFuelType', 'description'],
    fields: [S('name', 'Name', true), S('defaultFuelType', 'Default fuel type'), TA('description', 'Description')] },
  { group: 'Fleet', path: 'vehicle-documents', title: 'Vehicle documents', read: 'FLEET_READ', write: 'FLEET_WRITE', cols: ['vehicleId', 'docType', 'docNumber', 'expiryDate'],
    fields: [req(VEHICLE), SEL('docType', 'Type', ['REGISTRATION', 'INSURANCE', 'PERMIT', 'FITNESS', 'EMISSION', 'OTHER'], true), S('docNumber', 'Number'), S('provider', 'Provider'), D('issueDate', 'Issue date'), D('expiryDate', 'Expiry date'), { key: 'fileUrl', label: 'File', type: 'file', purpose: 'DOCUMENT' }] },
  { group: 'Fleet', path: 'vehicle-inspections', title: 'Inspections', read: 'FLEET_READ', write: 'FLEET_WRITE', cols: ['vehicleId', 'inspectedOn', 'inspectorName', 'result'],
    fields: [req(VEHICLE), D('inspectedOn', 'Inspected on', true), S('inspectorName', 'Inspector'), SEL('result', 'Result', ['PASS', 'FAIL', 'NEEDS_ATTENTION']), N('odometerKm', 'Odometer (km)'), TA('notes', 'Notes')] },

  { group: 'Drivers', path: 'drivers', title: 'Drivers', read: 'DRIVER_READ', write: 'DRIVER_WRITE', statusOptions: ['AVAILABLE', 'ON_TRIP', 'OFF_DUTY', 'SUSPENDED'], cols: ['fullName', 'mobile', 'licenceNumber', 'licenceExpiry', 'status'],
    fields: [S('fullName', 'Full name', true), S('mobile', 'Mobile'), S('email', 'Email'), N('userId', 'Linked user ID', false, { hint: 'The user account the driver logs into the mobile app with (see Users).' }), S('licenceNumber', 'Licence number'), S('licenceClass', 'Licence class'),
      D('licenceExpiry', 'Licence expiry'), SEL('status', 'Status', ['AVAILABLE', 'ON_TRIP', 'OFF_DUTY', 'SUSPENDED'], true), BRANCH, D('dateOfBirth', 'Date of birth'), S('address', 'Address'), D('joinedOn', 'Joined on')] },
  { group: 'Drivers', path: 'driver-expenses', title: 'Driver expenses', read: 'DRIVER_READ', write: 'DRIVER_WRITE', cols: ['driverId', 'expenseDate', 'category', 'amount'],
    fields: [req(DRIVER), D('expenseDate', 'Date', true), S('category', 'Category'), M('amount', 'Amount', true), S('description', 'Description')] },
  { group: 'Drivers', path: 'driver-accidents', title: 'Accident history', read: 'DRIVER_READ', write: 'DRIVER_WRITE', cols: ['driverId', 'vehicleId', 'occurredOn', 'severity', 'costEstimate'],
    fields: [req(DRIVER), VEHICLE, D('occurredOn', 'Date', true), S('location', 'Location'), SEL('severity', 'Severity', ['MINOR', 'MODERATE', 'MAJOR']), M('costEstimate', 'Estimated cost'), TA('description', 'Description')] },

  { group: 'Operations', path: 'journeys', title: 'Journeys', read: 'JOURNEY_READ', write: 'JOURNEY_WRITE', statusOptions: ['PLANNED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'],
    cols: ['journeyCode', 'startLocation', 'destination', 'driverId', 'vehicleId', 'plannedStart', 'status'],
    fields: [S('journeyCode', 'Journey code', false, { hint: 'Generated automatically if left empty.' }), VEHICLE, DRIVER, S('startLocation', 'Start location', true), N('startLat', 'Start latitude'), N('startLng', 'Start longitude'),
      S('destination', 'Destination', true), N('destLat', 'Destination latitude'), N('destLng', 'Destination longitude'), DT('plannedStart', 'Planned start'), DT('plannedEnd', 'Planned end'),
      N('plannedDistanceKm', 'Planned distance (km)'), N('estimatedFuelLitres', 'Estimated fuel (L)'), DT('etaAt', 'ETA'), SEL('status', 'Status', ['PLANNED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'], true), TA('notes', 'Notes')] },
  { group: 'Operations', path: 'journey-stops', title: 'Journey stops', read: 'JOURNEY_READ', write: 'JOURNEY_WRITE', cols: ['journeyId', 'sequenceNo', 'location', 'plannedArrival', 'status'],
    fields: [req(REF('journeyId', 'Journey', 'journeys', 'journeyCode')), N('sequenceNo', 'Sequence', true), S('location', 'Location', true), N('lat', 'Latitude'), N('lng', 'Longitude'), DT('plannedArrival', 'Planned arrival'), DT('actualArrival', 'Actual arrival'), SEL('status', 'Status', ['PENDING', 'ARRIVED', 'SKIPPED'], true)] },
  { group: 'Operations', path: 'routes', title: 'Saved routes', read: 'ROUTE_READ', write: 'ROUTE_WRITE', cols: ['name', 'mode', 'distanceKm', 'durationMinutes', 'tollEstimate'],
    fields: [S('name', 'Name', true), REF('journeyId', 'Journey', 'journeys', 'journeyCode'), S('originName', 'Origin'), S('destinationName', 'Destination'), SEL('mode', 'Mode', ['FASTEST', 'SHORTEST', 'FUEL_EFFICIENT'], true), N('distanceKm', 'Distance (km)'), N('durationMinutes', 'Duration (min)'), M('tollEstimate', 'Toll estimate'), N('fuelEstimateLitres', 'Fuel estimate (L)')] },
  { group: 'Operations', path: 'shipments', title: 'Shipments', read: 'SHIPMENT_READ', write: 'SHIPMENT_WRITE', statusOptions: ['CREATED', 'ASSIGNED', 'PICKED_UP', 'IN_TRANSIT', 'OUT_FOR_DELIVERY', 'DELIVERED', 'FAILED', 'RETURNED', 'CANCELLED'],
    rowActions: 'shipment', cols: ['trackingId', 'customerId', 'destinationAddress', 'driverId', 'scheduledDelivery', 'status'],
    fields: [CUST, S('pickupAddress', 'Pickup address', true), N('pickupLat', 'Pickup latitude'), N('pickupLng', 'Pickup longitude'), S('destinationAddress', 'Destination address', true), N('destLat', 'Destination latitude'), N('destLng', 'Destination longitude'),
      S('receiverName', 'Receiver name'), S('receiverMobile', 'Receiver mobile (+countrycode)'), S('packageDescription', 'Package description'), N('weightKg', 'Weight (kg)'), N('volumeCbm', 'Volume (m³)'), N('packageCount', 'Packages'),
      M('declaredValue', 'Declared value'), M('price', 'Price'), DT('scheduledPickup', 'Scheduled pickup'), DT('scheduledDelivery', 'Scheduled delivery'), WH, REF('journeyId', 'Journey', 'journeys', 'journeyCode'), B('returnShipment', 'Return shipment'), TA('notes', 'Notes')],
    note: 'Status changes, assignment and proof of delivery use the row actions so that the customer timeline and driver earnings stay correct.' },
  { group: 'Operations', path: 'shipment-events', title: 'Shipment events', read: 'SHIPMENT_READ', write: 'SHIPMENT_WRITE', cols: ['shipmentId', 'status', 'note', 'occurredAt', 'publicVisible'],
    fields: [req(REF('shipmentId', 'Shipment', 'shipments', 'trackingId')), S('status', 'Status', true), S('note', 'Note'), DT('occurredAt', 'Occurred at', true), B('publicVisible', 'Visible to customer')] },
  { group: 'Operations', path: 'geofences', title: 'Geofences', read: 'GPS_READ', write: 'ROUTE_WRITE', cols: ['name', 'centerLat', 'centerLng', 'radiusMeters', 'active'],
    fields: [S('name', 'Name', true), N('centerLat', 'Center latitude', true), N('centerLng', 'Center longitude', true), N('radiusMeters', 'Radius (m)', true), B('active', 'Active')] },

  { group: 'Fuel', path: 'fuel-records', title: 'Fuel records', read: 'FUEL_READ', write: 'FUEL_WRITE', cols: ['vehicleId', 'fuelDate', 'litres', 'totalCost', 'odometerKm', 'station'],
    fields: [req(VEHICLE), DRIVER, REF('journeyId', 'Journey', 'journeys', 'journeyCode'), VENDOR, DT('fuelDate', 'Date', true), N('litres', 'Litres', true), N('pricePerLitre', 'Price per litre'), M('totalCost', 'Total cost', true), N('odometerKm', 'Odometer (km)'), S('station', 'Station')] },
  { group: 'Fuel', path: 'fuel-prices', title: 'Fuel prices', read: 'FUEL_READ', write: 'FUEL_WRITE', cols: ['fuelType', 'pricePerLitre', 'effectiveFrom', 'region'],
    fields: [SEL('fuelType', 'Fuel type', ['DIESEL', 'PETROL', 'CNG', 'LPG'], true), N('pricePerLitre', 'Price per litre', true), D('effectiveFrom', 'Effective from', true), S('region', 'Region')] },

  { group: 'Maintenance', path: 'maintenance', title: 'Maintenance', read: 'MAINTENANCE_READ', write: 'MAINTENANCE_WRITE', statusOptions: ['OPEN', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'], cols: ['vehicleId', 'kind', 'title', 'scheduledOn', 'cost', 'status'],
    fields: [req(VEHICLE), SEL('kind', 'Kind', ['SCHEDULED', 'PREVENTIVE', 'REPAIR', 'BREAKDOWN', 'TYRE', 'BATTERY'], true), S('title', 'Title', true), TA('description', 'Description'), SEL('status', 'Status', ['OPEN', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'], true),
      D('scheduledOn', 'Scheduled on'), D('completedOn', 'Completed on'), N('odometerKm', 'Odometer (km)'), REF('serviceCentreId', 'Service centre', 'vendors', 'name'), M('cost', 'Cost'), D('nextDueDate', 'Next due date'), N('nextDueKm', 'Next due (km)')] },
  { group: 'Maintenance', path: 'spare-parts', title: 'Spare parts', read: 'MAINTENANCE_READ', write: 'MAINTENANCE_WRITE', cols: ['name', 'sku', 'quantity', 'reorderLevel', 'unitCost'],
    fields: [S('name', 'Name', true), S('sku', 'SKU'), VENDOR, N('quantity', 'Quantity'), M('unitCost', 'Unit cost'), N('reorderLevel', 'Reorder level')] },

  { group: 'People', path: 'employees', title: 'Employees', read: 'EMPLOYEE_READ', write: 'EMPLOYEE_WRITE', cols: ['employeeCode', 'fullName', 'jobTitle', 'mobile', 'status'],
    fields: [S('employeeCode', 'Employee code', false, { hint: 'Generated automatically if left empty.' }), S('fullName', 'Full name', true), S('email', 'Email'), S('mobile', 'Mobile'), N('userId', 'Linked user ID', false, { hint: 'Lets the employee see their own payslips and attendance in the apps.' }),
      DRIVER, REF('departmentId', 'Department', 'departments', 'name'), BRANCH, REF('shiftId', 'Shift', 'shifts', 'name'), S('jobTitle', 'Job title'), SEL('status', 'Status', ['ACTIVE', 'INACTIVE', 'ON_LEAVE'], true), D('joinedOn', 'Joined on')] },
  { group: 'People', path: 'shifts', title: 'Shifts', read: 'EMPLOYEE_READ', write: 'EMPLOYEE_WRITE', cols: ['name', 'startTime', 'endTime'], fields: [S('name', 'Name', true), S('startTime', 'Start (HH:MM)', true), S('endTime', 'End (HH:MM)', true)] },
  { group: 'People', path: 'attendance', title: 'Attendance', read: 'EMPLOYEE_READ', write: 'EMPLOYEE_WRITE', cols: ['employeeId', 'workDate', 'checkIn', 'checkOut', 'status'],
    fields: [req(EMP), D('workDate', 'Date', true), DT('checkIn', 'Check in'), DT('checkOut', 'Check out'), SEL('status', 'Status', ['PRESENT', 'LATE', 'ABSENT', 'ON_LEAVE'], true), S('notes', 'Notes')] },
  { group: 'People', path: 'leave-requests', title: 'Leave', read: 'EMPLOYEE_READ', write: 'EMPLOYEE_WRITE', cols: ['employeeId', 'leaveType', 'fromDate', 'toDate', 'status'],
    quickActions: [{ label: 'Approve', patch: { status: 'APPROVED' }, when: (r) => r.status === 'PENDING' }, { label: 'Reject', patch: { status: 'REJECTED' }, when: (r) => r.status === 'PENDING' }],
    fields: [req(EMP), SEL('leaveType', 'Type', ['ANNUAL', 'SICK', 'UNPAID', 'OTHER'], true), D('fromDate', 'From', true), D('toDate', 'To', true), SEL('status', 'Status', ['PENDING', 'APPROVED', 'REJECTED'], true), S('reason', 'Reason')] },
  { group: 'People', path: 'tasks', title: 'Tasks', read: 'EMPLOYEE_READ', write: 'EMPLOYEE_WRITE', cols: ['title', 'assignedEmployeeId', 'dueDate', 'priority', 'status'],
    fields: [S('title', 'Title', true), TA('description', 'Description'), REF('assignedEmployeeId', 'Assigned to', 'employees', 'fullName'), D('dueDate', 'Due date'), SEL('priority', 'Priority', ['LOW', 'NORMAL', 'HIGH']), SEL('status', 'Status', ['OPEN', 'IN_PROGRESS', 'DONE'], true)] },

  { group: 'Payroll', path: 'salary-structures', title: 'Salary structures', read: 'PAYROLL_READ', write: 'PAYROLL_WRITE', cols: ['employeeId', 'model', 'monthlySalary', 'perDeliveryRate', 'perTripRate'],
    note: 'Each employee can use a different salary model: FIXED (monthly) or WORK_BASED (per delivery, trip, distance or task).',
    fields: [req(EMP), SEL('model', 'Model', ['FIXED', 'WORK_BASED'], true), M('monthlySalary', 'Monthly salary'), M('overtimeRate', 'Overtime rate / hour'), M('perDeliveryRate', 'Per delivery'), M('perTripRate', 'Per trip'), N('perKmRate', 'Per km'), M('perTaskRate', 'Per task'), D('effectiveFrom', 'Effective from')] },
  { group: 'Payroll', path: 'work-records', title: 'Work records', read: 'PAYROLL_READ', write: 'PAYROLL_WRITE', cols: ['employeeId', 'workType', 'quantity', 'amount', 'workDate', 'status'],
    note: 'Completed deliveries and trips create PENDING records. A manager must approve them before they are paid.',
    quickActions: [{ label: 'Approve', post: (r) => `/payroll/work-records/${r.id}/approve`, when: (r) => r.status === 'PENDING' }, { label: 'Reject', post: (r) => `/payroll/work-records/${r.id}/reject`, when: (r) => r.status === 'PENDING' }],
    fields: [req(EMP), SEL('workType', 'Type', ['DELIVERY', 'TRIP', 'DISTANCE', 'TASK'], true), N('referenceId', 'Reference ID'), N('quantity', 'Quantity', true), M('rate', 'Rate'), M('amount', 'Amount'), D('workDate', 'Date', true)] },
  { group: 'Payroll', path: 'salary-advances', title: 'Salary advances', read: 'PAYROLL_READ', write: 'PAYROLL_WRITE', cols: ['employeeId', 'amount', 'repaid', 'advanceDate', 'status'],
    fields: [req(EMP), M('amount', 'Amount', true), D('advanceDate', 'Date', true)] },
  { group: 'Payroll', path: 'payslips', title: 'Payslips', read: 'PAYROLL_READ', write: 'PAYROLL_WRITE', noCreate: true, cols: ['periodMonth', 'employeeId', 'basic', 'workEarnings', 'netSalary', 'status'],
    note: 'Payslips are produced from Payroll runs. You can adjust bonus and deductions on a DRAFT payslip before finalising.',
    fields: [M('bonus', 'Bonus'), M('deductions', 'Deductions')] },

  { group: 'Customers', path: 'customers', title: 'Customers', read: 'CUSTOMER_READ', write: 'CUSTOMER_WRITE', cols: ['name', 'companyName', 'email', 'mobile', 'status'],
    fields: [S('name', 'Name', true), S('companyName', 'Company'), S('email', 'Email'), S('mobile', 'Mobile'), S('taxId', 'Tax ID'), SEL('status', 'Status', ['ACTIVE', 'INACTIVE'], true), TA('notes', 'Notes')] },
  { group: 'Customers', path: 'customer-addresses', title: 'Customer addresses', read: 'CUSTOMER_READ', write: 'CUSTOMER_WRITE', cols: ['customerId', 'label', 'line1', 'city', 'country'],
    fields: [req(CUST), S('label', 'Label'), S('line1', 'Address', true), S('city', 'City'), S('state', 'State'), S('postalCode', 'Postal code'), S('country', 'Country'), N('latitude', 'Latitude'), N('longitude', 'Longitude')] },
  { group: 'Customers', path: 'customer-feedback', title: 'Feedback and complaints', read: 'CUSTOMER_READ', write: 'CUSTOMER_WRITE', cols: ['customerId', 'shipmentId', 'kind', 'rating', 'comment', 'status'],
    fields: [CUST, REF('shipmentId', 'Shipment', 'shipments', 'trackingId'), SEL('kind', 'Kind', ['FEEDBACK', 'COMPLAINT'], true), N('rating', 'Rating (1-5)'), TA('comment', 'Comment'), SEL('status', 'Status', ['OPEN', 'IN_PROGRESS', 'RESOLVED'], true)] },

  { group: 'Warehouse', path: 'inventory-items', title: 'Inventory', read: 'WAREHOUSE_READ', write: 'WAREHOUSE_WRITE', cols: ['warehouseId', 'sku', 'name', 'barcode', 'quantity'],
    fields: [req(WH), S('sku', 'SKU', true), S('name', 'Name', true), S('barcode', 'Barcode / QR value'), N('quantity', 'Quantity'), S('unit', 'Unit'), N('reorderLevel', 'Reorder level')] },
  { group: 'Warehouse', path: 'stock-transfers', title: 'Stock transfers', read: 'WAREHOUSE_READ', write: 'WAREHOUSE_WRITE', cols: ['fromWarehouseId', 'toWarehouseId', 'itemId', 'quantity', 'transferredAt'],
    fields: [req({ ...WH, key: 'fromWarehouseId', label: 'From warehouse' }), req({ ...WH, key: 'toWarehouseId', label: 'To warehouse' }), req(REF('itemId', 'Item', 'inventory-items', 'name')), N('quantity', 'Quantity', true), DT('transferredAt', 'Transferred at')] },
  { group: 'Warehouse', path: 'inventory-history', title: 'Inventory history', read: 'WAREHOUSE_READ', write: 'WAREHOUSE_WRITE', cols: ['itemId', 'changeQty', 'reason', 'balanceAfter', 'occurredAt'],
    fields: [req(REF('itemId', 'Item', 'inventory-items', 'name')), N('changeQty', 'Change', true), S('reason', 'Reason'), N('balanceAfter', 'Balance after'), DT('occurredAt', 'Date', true)] },

  { group: 'Vendors', path: 'vendors', title: 'Vendors', read: 'VENDOR_READ', write: 'VENDOR_WRITE', cols: ['name', 'vendorType', 'contactName', 'mobile', 'active'],
    fields: [S('name', 'Name', true), SEL('vendorType', 'Type', ['FUEL', 'SERVICE', 'PARTS', 'INSURANCE', 'TRANSPORT'], true), S('contactName', 'Contact'), S('email', 'Email'), S('mobile', 'Mobile'), S('address', 'Address'), S('taxId', 'Tax ID'), B('active', 'Active')] },
  { group: 'Vendors', path: 'purchase-orders', title: 'Purchase orders', read: 'VENDOR_READ', write: 'VENDOR_WRITE', statusOptions: ['DRAFT', 'ORDERED', 'RECEIVED', 'CANCELLED'], cols: ['poNumber', 'vendorId', 'orderDate', 'totalAmount', 'status'],
    fields: [S('poNumber', 'PO number', false, { hint: 'Generated automatically if left empty.' }), req(VENDOR), D('orderDate', 'Order date', true), SEL('status', 'Status', ['DRAFT', 'ORDERED', 'RECEIVED', 'CANCELLED'], true), M('totalAmount', 'Total'), TA('notes', 'Notes')] },

  { group: 'Finance', path: 'invoices', title: 'Invoices', read: 'FINANCE_READ', write: 'FINANCE_WRITE', statusOptions: ['DRAFT', 'SENT', 'PAID', 'OVERDUE', 'VOID'], cols: ['invoiceNumber', 'kind', 'customerId', 'issueDate', 'dueDate', 'total', 'status'],
    fields: [S('invoiceNumber', 'Invoice number', false, { hint: 'Generated automatically if left empty.' }), SEL('kind', 'Kind', ['CUSTOMER', 'VENDOR'], true), CUST, VENDOR, REF('shipmentId', 'Shipment', 'shipments', 'trackingId'), D('issueDate', 'Issue date', true), D('dueDate', 'Due date'),
      M('subtotal', 'Subtotal'), M('tax', 'Tax'), M('total', 'Total', true), SEL('status', 'Status', ['DRAFT', 'SENT', 'PAID', 'OVERDUE', 'VOID'], true), S('currency', 'Currency (3 letters)'), TA('notes', 'Notes')] },
  { group: 'Finance', path: 'payments', title: 'Payments', read: 'FINANCE_READ', write: 'FINANCE_WRITE', cols: ['direction', 'invoiceId', 'amount', 'method', 'paidAt'],
    fields: [SEL('direction', 'Direction', ['IN', 'OUT'], true), REF('invoiceId', 'Invoice', 'invoices', 'invoiceNumber'), M('amount', 'Amount', true), S('method', 'Method'), S('reference', 'Reference'), DT('paidAt', 'Paid at', true)] },
  { group: 'Finance', path: 'expenses', title: 'Expenses', read: 'FINANCE_READ', write: 'FINANCE_WRITE', cols: ['category', 'amount', 'expenseDate', 'vehicleId', 'description'],
    fields: [SEL('category', 'Category', ['FUEL', 'MAINTENANCE', 'DRIVER', 'SALARY', 'VENDOR', 'OTHER'], true), M('amount', 'Amount', true), D('expenseDate', 'Date', true), VEHICLE, VENDOR, S('description', 'Description')] },

  { group: 'Support', path: 'support-tickets', title: 'Support tickets', read: 'SUPPORT_MANAGE', write: 'SUPPORT_MANAGE', cols: ['category', 'subject', 'priority', 'status', 'createdAt'],
    note: 'Tickets are visible to the FleetPulse team in the Super Admin portal.',
    fields: [SEL('category', 'Category', ['SUPPORT', 'BUG', 'FEATURE', 'FEEDBACK'], true), S('subject', 'Subject', true), TA('description', 'Description'), SEL('priority', 'Priority', ['LOW', 'NORMAL', 'HIGH', 'URGENT'], true)] },
];

export const groupsOrder = ['Organization', 'Fleet', 'Drivers', 'Operations', 'Fuel', 'Maintenance', 'People', 'Payroll', 'Customers', 'Warehouse', 'Vendors', 'Finance', 'Support'];
