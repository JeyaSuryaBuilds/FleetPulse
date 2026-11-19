import { Truck, Users, Package, MapPinned, Route, Fuel, Wrench, UserCog, Wallet, Warehouse, Building2, Sparkles, FileBarChart, Bell, ShieldCheck, CreditCard } from 'lucide-react';

export const modules = [
  { icon: Truck, title: 'Fleet management', text: 'Vehicle register, categories, documents, inspections, assignment, availability, purchase, resale and depreciation, with fleet utilisation reports.' },
  { icon: Users, title: 'Driver management', text: 'Driver profiles, licence and expiry tracking, assignment, attendance, performance, expenses and accident history.' },
  { icon: Route, title: 'Journeys and routes', text: 'Multi-stop journeys with ETA, planned vs actual, shortest, fastest and fuel-aware route planning and stop ordering on real road networks.' },
  { icon: Package, title: 'Shipments', text: 'Tracking IDs, pickup and delivery, driver and vehicle assignment, proof of delivery, failed deliveries, returns and a full timeline.' },
  { icon: MapPinned, title: 'Live GPS tracking', text: 'Real positions from the driver app, movement history, geofences, route deviation and unauthorised stop detection, plus customer tracking links.' },
  { icon: Fuel, title: 'Fuel management', text: 'Fuel records and prices, mileage and consumption, efficiency reports and alerts for unusual consumption.' },
  { icon: Wrench, title: 'Maintenance', text: 'Scheduled and preventive service, repair requests, breakdowns, service centres, spare parts, tyres, batteries and service history.' },
  { icon: UserCog, title: 'Employees and payroll', text: 'Attendance, shifts, leave and tasks. Fixed and work-based salary models with approval, payslips and payment history.' },
  { icon: Building2, title: 'Customers and vendors', text: 'Customer profiles, addresses, pricing, invoices and feedback. Fuel, service, parts, insurance and transport vendors with purchase orders.' },
  { icon: Warehouse, title: 'Warehouses', text: 'Warehouses, inventory, incoming and outgoing shipments, QR and barcode lookups, stock transfers and history.' },
  { icon: Wallet, title: 'Finance', text: 'Revenue, expenses, invoices, payments, outstanding balances and profit and loss, with PDF and CSV export.' },
  { icon: Sparkles, title: 'AI intelligence', text: 'A fleet assistant for fuel, delay risk, maintenance, driver and expense insights. It only sees your company data that your role may access.' },
  { icon: FileBarChart, title: 'Reports', text: 'Fleet, vehicle, driver, shipment, fuel, maintenance, employee, payroll, financial and customer reports as PDF or CSV.' },
  { icon: Bell, title: 'Alerts', text: 'Licence, insurance and document expiry, maintenance due, overdue payments and delayed shipments.' },
  { icon: ShieldCheck, title: 'Roles and security', text: 'Eight roles with permissions enforced on the server, isolated company workspaces and audit logs.' },
  { icon: CreditCard, title: 'Subscription', text: 'Free trial, plan limits for vehicles, users, AI and GPS, upgrade or downgrade and billing history.' },
];

export const solutions = [
  { title: 'Logistics companies', text: 'Coordinate multi-branch operations, dispatch shipments, and see cost and margin per vehicle and customer.' },
  { title: 'Transportation companies', text: 'Keep vehicles compliant and productive with document expiry alerts, maintenance planning and utilisation figures.' },
  { title: 'Courier companies', text: 'Assign deliveries, capture proof of delivery from the driver app and give recipients a live tracking link.' },
  { title: 'Delivery companies', text: 'Plan multi-stop routes, monitor delays and pay drivers per delivery with manager approval.' },
  { title: 'Fleet operators', text: 'Track fuel, maintenance, depreciation and driver performance across every asset.' },
  { title: 'Warehouse and distribution', text: 'Manage stock across warehouses, transfers, inbound and outbound shipments with barcode lookups.' },
];

export const benefits = [
  ['One system', 'Vehicles, drivers, shipments, payroll and finance share the same data, so nothing is re-typed.'],
  ['Real tracking', 'Positions come from your drivers’ phones. If there is no recent signal, FleetPulse says so instead of guessing.'],
  ['Controlled access', 'Each role sees only what it needs, and every company workspace is fully isolated.'],
  ['Every device', 'Web, Windows desktop, Android and iPhone use the same backend.'],
];

export const faqs = [
  ['Is there a free trial?', 'Yes. New companies start on a trial of the plan they choose. No manual approval is needed: verify your email and mobile number and your workspace is created immediately.'],
  ['How does GPS tracking work?', 'Drivers share their location from the FleetPulse mobile app. Tracking shows real reported positions only, and flags vehicles that have not reported recently.'],
  ['Can customers track their shipments?', 'Yes. Anyone with a tracking ID can open the public tracking page to see status, timeline and, when real GPS data is available, the current location.'],
  ['Which roles are available?', 'Company Admin, Operations Manager, Fleet Manager, Accountant, HR Manager, Driver, Warehouse Manager and Customer Support.'],
  ['Can I export my data?', 'Reports can be exported as PDF or CSV.'],
  ['Is my company data separated from other companies?', 'Yes. Every request is checked on the server against your company, regardless of what the screen shows.'],
];

export const businessTypes = ['Logistics', 'Transportation', 'Courier', 'Last-mile delivery', 'Fleet operators', 'Warehousing and distribution'];
