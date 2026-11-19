# FLEETPULSE_PROGRESS.md

Status: IN PROGRESS (session 6 done). Backend and web app are written; desktop (Tauri), mobile (Expo) and docs remain. Nothing has been built or run. Nothing has been built, run or tested (per instructions). All code is unverified static output.

## Repository layout
- `backend/`  Spring Boot 3.3 / Java 21 / Maven / MySQL / Flyway
- `web/`      Vite + React + Tailwind v3 (public site, company admin, super admin `/admin`, tracking)
- `desktop/`  Tauri + React (only brand icon source copied so far)
- `mobile/`   Expo React Native (brand assets copied to `mobile/assets/`)
- `brand-kit/` untouched original kit; `brand-kit-extracted/` raw unzip (can be deleted)
- `.env.example`

## Completed
### Brand kit integration (web)
- Copied assets, styles, design-system, components into `web/src`; public/ (favicon, icons, manifest); tailwind.preset.js
- `web/index.html` has favicon/manifest/theme-color and `data-theme="dark"`; `main.jsx` imports styles + `FleetPulseMotion`
- `web/package.json`, `vite.config.js` (proxy /api to :8080), `postcss.config.js`, `tailwind.config.js`, `src/api.js` (axios, token refresh, separate admin session), placeholder `App.jsx`
### Mobile / desktop assets
- App icons, splash mark, adaptive icons, `tokens.native.ts` copied to `mobile/assets/`; Tauri source icon in `desktop/app-icons/`
### Backend (static code only)
- `pom.xml`, `application.yml` (all secrets from env)
- Flyway `V1__core_platform_tenancy.sql`: plans (3 seeded), companies, roles (8 seeded), permissions, role_permissions, users, refresh_tokens, pending_registrations, subscriptions, platform_admins, login_activity, audit_logs
- Security: `JwtService`, `JwtAuthFilter` (re-checks token_version, user + company status in DB per request), `RateLimitFilter` (in-memory, per IP), `SecurityConfig`, `Role`/`Permission` enums
- Tenancy: `TenantContext`, `TenantAwareEntity` (stamps company_id + Hibernate filter), `TenantFilterAspect`, `TenantGuard`
- Auth: login (lockout, audit), refresh rotation, logout, `/auth/me`
- Registration: start -> email OTP + SMS OTP -> auto-create company, Company Admin, trial subscription, return session (no approval step)
- OTP delivery: SMTP and Twilio only; returns 503 if unconfigured (never fakes sending)
- Super Admin: `POST /api/v1/admin/auth/login` (password only), lockout, audit, `logout-all`; `SuperAdminBootstrap` hashes the initial password from `FP_SUPERADMIN_BOOTSTRAP_PASSWORD` at first start
- `GET /api/v1/public/plans` (from DB)
- Global exception handler, validation, OpenAPI via springdoc (`/swagger-ui.html`)

## Architectural decisions
- Identity/registration layer uses JdbcTemplate; domain modules should use JPA entities extending `TenantAwareEntity`
- Hibernate filters do NOT apply to `findById`: always load via `TenantGuard.owned(...)` or repositories taking `companyId`
- Super Admin password is never in source; only a BCrypt hash is stored. Remove the bootstrap env var after first start and change the password. A single shared password has no per-person accountability, so treat it as temporary
- Domain tables are generated; every tenant table has company_id, FK to companies, and an index. Unique constraints are per company
- Role permissions live in code (`Role` enum); DB roles/permissions tables are seeded for visibility only (permissions table not yet populated)
- Rate limiter is in-memory (single instance)

## Known issues / to verify on your laptop
- Nothing compiled. Expect small compile errors; `TenantFilterAspect` ordering relative to the transaction advice should be checked
- `permissions`/`role_permissions` tables are empty
- Need `FP_JWT_SECRET`, DB credentials, SMTP and Twilio for registration to work

### Session 2 additions (backend domain layer, static code only)
- `tools/gen_domain.py` generates `V2__domain_modules.sql` (43 tenant tables + `gps_locations` + `ai_usage`) and, per table, a JPA entity, repository and controller in `backend/.../domain/`. Edit the generator, not the output.
- Generic `CrudController`: transactional (required so the tenant filter is on), per-permission checks, pagination (max 100), equality filters + `?q=` search + `sort`, audit log on create/update/delete, and cross-tenant reference checks (`fk` fields must belong to the same company).
- Endpoints under `/api/v1/`: branches, departments, warehouses, vehicle-categories, drivers, vehicles (plan limit enforced), vehicle-documents, vehicle-inspections, customers, customer-addresses, vendors, journeys, journey-stops, routes, driver-expenses, driver-accidents, shipments (random tracking IDs), shipment-events, geofences, fuel-records, fuel-prices, maintenance, spare-parts, purchase-orders, shifts, employees, attendance, leave-requests, tasks, salary-structures, work-records, salary-advances, payroll-runs, payslips, inventory-items, stock-transfers, inventory-history, invoices, payments, expenses, notifications, support-tickets, customer-feedback
- Hand-written: `GpsController` (driver ping publish, live positions with stale flag, history, geofence exits), `PublicTrackingController` (`GET /api/v1/public/tracking/{id}`, customer-safe fields, location only from real recent GPS), `DashboardController` (KPIs from real rows), `CompanyController` (profile, subscription + usage, cancel), `UserAdminController` (company users/roles, last-admin guard), `PlanLimits`
- `application.yml`: `ddl-auto: none` (Flyway owns schema)

### Session 3 additions (backend, static code only)
- `V3__platform_management.sql`: platform_invoices, platform_payments, platform_settings (seeded), feature_flags (seeded), announcements, ai_models
- Super Admin API (all `/api/v1/admin/**`, ROLE_SUPER_ADMIN only): dashboard (companies by status, users, vehicles, MRR/ARR from ACTIVE subscriptions, subscription revenue, AI usage), companies (list/detail/usage/activity, set status, change plan/cycle/trial/status; no approval flow), plans CRUD, finance (payments, invoices, refund REQUEST, tax report), AI (config without exposing the key, model registry, per-company usage + estimated cost), platform (health, maintenance mode, feature flags, settings with secret-key blocking, announcements, support tickets across companies), security (change password, login activity, audit logs, logout-all)
- `MaintenanceModeFilter`: 503 for tenant/public APIs while maintenance_mode is on
- Workflow: `ShipmentWorkflowService` + `POST /api/v1/shipments/{id}/status` (validated transitions, POD required for DELIVERED, reason required for FAILED, writes public timeline event, creates PENDING work record for work-based drivers)
- Self-service `/api/v1/me/**` (own records only): profile, journeys (+start/complete updates vehicle/driver status), shipments (+status, QR/barcode scan lookup), attendance check-in/out, payslips (FINALIZED/PAID only), earnings, fuel entry, driver expenses. All staff roles now have SELF_SERVICE + ATTENDANCE_SELF
- Payroll engine `/api/v1/payroll`: generate (fixed + work-based, overtime, absence deduction, advances), finalize (recovers advances), mark-paid (books SALARY expenses, marks work records PAID), approve/reject work records
- `AlertScheduler` (daily 03:15 UTC): licence/vehicle-document expiry, maintenance due, overdue customer invoices, delayed shipments -> `notifications`; expires lapsed trials
- `PermissionSeeder` fills permissions/role_permissions from the enums

### Session 4 additions (backend, static code only)
- Reports `/api/v1/reports` (catalogue, JSON, `/{type}/export?format=csv|pdf&from&to`): fleet, vehicle, driver, shipment, fuel, maintenance, employee, payroll, financial (P&L), customer. Fixed parameterised SQL bound to the caller's company; permission = REPORT_VIEW + module permission; CSV has formula-injection protection; PDF via OpenPDF with brand icon (`resources/brand/icon-192.png`)
- NVIDIA NIM `/api/v1/ai/status` and `/api/v1/ai/analyze` (features: ASSISTANT, FUEL_ANALYSIS, DELAY_PREDICTION, ROUTE_RECOMMENDATION, MAINTENANCE_INSIGHTS, DRIVER_PERFORMANCE, EXPENSE_ANALYSIS, REPORT). The model only receives fixed aggregate queries for the caller's company, filtered by the caller's permissions (no payroll, no contact details); it has no tools or DB access. Works only with `FP_NVIDIA_API_KEY` + Super Admin enabling AI and choosing a model; enforces plan quota and per-user rate limit; logs every call in `ai_usage`. Delay prediction is a statistical judgement by the model from historical figures, not a trained ML model, and the prompt says so
- Analytics `/api/v1/analytics`: depreciation (straight-line, book value, resale gain/loss), utilisation (active days / days), fuel efficiency + unusual consumption (full-tank odometer method; needs odometer readings)
- GPS monthly quota now enforced (`GpsQuota`, cached 60s); `V4__indexes_quota.sql`
- Shipments: generic create/edit blocked from setting status/delivery/POD fields (must use the workflow endpoint); CRUD hooks `beforeCreate/beforeUpdate(entity, body)`

### Session 5 additions (backend integrations, static code only)
- `V5__files_push_contact_billing.sql`: files, device_tokens, contact_requests, processed_webhook_events
- Files `/api/v1/files` (multipart upload, JPEG/PNG/WebP/PDF detected by content, 15 MB, tenant prefix; download only via authenticated endpoint; drivers only their own uploads). Storage: local disk (dev) or S3-compatible (`FP_STORAGE_TYPE=s3`). Entities store the returned `/api/v1/files/{id}` in their `*Url` fields
- Routing `/api/v1/route-planning/plan` and `/optimize` (OpenRouteService; needs `FP_ORS_API_KEY`): fastest / shortest / "fuel-efficient" (documented approximation), multi-stop, ETA, toll road km (toll price only if you pass a rate per km), fuel estimate only from the vehicle's measured consumption. Stop ordering = nearest neighbour + 2-opt over a real road-time matrix (max 25 stops). Save results through `POST /api/v1/routes` with the returned geometry (JSON `[[lat,lng],...]`) in `waypointsJson`
- `GET /api/v1/gps/journeys/{id}/monitor`: route deviation (needs a saved route geometry) and unauthorised stops from real pings
- Billing: `POST /api/v1/billing/checkout` (Stripe Checkout; price always from the plans table), `POST /api/v1/public/webhooks/stripe` (signature verified, idempotent; writes platform invoices/payments, activates/expires/cancels subscriptions), cancel at period end contacts Stripe for paid subscriptions. Needs `FP_STRIPE_SECRET_KEY` + `FP_STRIPE_WEBHOOK_SECRET`. Upgrade/downgrade/renewal all go through a new checkout; proration is not handled. Verify the invoice JSON field names (`subscription`, `tax`) against your Stripe API version because Stripe moved some fields in recent versions
- Push: `/api/v1/me/devices` (register Expo push token), `/api/v1/me/notifications`, `PushService` (Expo Push API). `POST /api/v1/shipments/{id}/assign` assigns driver/vehicle and notifies the driver. FCM/APNs credentials are configured in Expo/EAS
- WhatsApp: `WhatsAppService` sends approved templates on OUT_FOR_DELIVERY / DELIVERED only when env + feature flag `whatsapp_notifications` are set (silent no-op otherwise)
- Public: `POST /api/v1/public/contact` (demo/contact requests, per-IP limit), `POST /api/v1/public/tracking/{id}/feedback` (delivered shipments only), Super Admin `/admin/contact-requests`, company `/company/announcements`

### Session 6 additions (web app in `web/`, static code only)
- Stack: Vite + React 18 + Tailwind v3 (brand preset) + React Router + Axios + Recharts + Lucide + Framer Motion + Leaflet (OSM tiles). `npm install && npm run dev` (proxy `/api` -> :8080)
- Core: `src/api.js` (bearer token, refresh rotation, separate Super Admin session in sessionStorage), `auth.jsx` (UI guards only; the backend re-authorises every call), `ui.jsx` (Button, Field, Modal, Toast, StatusBadge, Stat...), `nav.js`, `resources.js`
- Public site: Home (hero image slot `public/images/hero-fleet.jpg`, labelled product preview, features, benefits, business types, pricing preview from `/public/plans`, FAQ), Features, Solutions, Pricing (monthly/annual from backend), About, Contact/Demo, Login, Register (details -> email OTP + mobile OTP -> auto workspace), public Tracking page (+ feedback after delivery), `/admin` password-only login
- Company portal `/app` (permission-aware nav): Dashboard (KPIs, charts, expiring docs/licences, activity), My work and pay (own payslips/earnings/attendance), Live map (real pings only, stale/no-signal listed separately), Notifications + announcements, AI assistant, Reports (PDF/CSV), Fleet analytics (utilisation, fuel efficiency + anomalies, depreciation), Route planner (plan, optimise, save), Payroll runs (generate/finalise/mark paid), Company profile, Users and roles, Subscription (usage meters, Stripe checkout, cancel, invoices)
- 40 standard modules via one generic `ResourcePage` driven by `resources.js` (search, status filter, pagination, create/edit/delete, ref selects, file upload, quick actions). Shipments have Assign / Update status dialogs that call the workflow endpoints
- Super Admin `/admin/*`: Dashboard, Companies (detail, suspend/reactivate, plan/trial change), Plans, Finance (payments, invoices, tax, refund request), AI management, Platform (health, maintenance mode, flags), Customer success (tickets, website requests, announcements), Security (change password, sessions, login activity, audit logs), Configuration (non-secret settings)
- Not possible for me to supply: photorealistic 3D fleet renders. The hero shows `public/images/hero-fleet.jpg` if you add one, otherwise a neutral brand panel

## Payroll assumptions (confirm with your HR/legal rules)
Working days = Mon-Fri; 8h standard day; absent days deducted at monthly/working days; overtime = hours over 8 x rate; advances recovered in full (never below zero); work-based pay only from APPROVED records.

## Known gaps in what exists (fix next)
- Other status fields (vehicles, journeys, maintenance, invoices...) are still free text; add allowed-value validation
- Not built yet: scheduled notifications for unusual fuel consumption (endpoint exists), employee-facing leave requests, customer feedback submission by customers, generic write access to `notifications` read-state per user
- Geofence "route deviation" is only a feed of pings outside geofences; real deviation needs a stored route polyline from a routing provider

## Remaining (next task: item 1)
1. Desktop: Tauri 2 project in `desktop/` reusing `web/src` (copy or alias), `src-tauri` (Cargo.toml, tauri.conf.json with NSIS bundle, icons from brand kit, capabilities), API base URL config, secure session storage, build instructions. Windows EXE must be built on Windows (`npm run tauri build`); no EXE exists yet
2. Mobile: Expo SDK app in `mobile/` (TypeScript): login, driver dashboard, journeys (start/complete), shipments (status, POD with camera, QR/barcode scan), GPS foreground + background location (expo-location/task-manager) posting to `/gps/locations`, push (expo-notifications -> `/me/devices`), fuel + expenses, attendance, profile, payslips/earnings; `app.json`, `eas.json` (APK + AAB profiles, iOS profile), splash/icons from brand kit. No IPA/APK exists yet
3. Docs: root README (setup, env, run order, first-login steps), API overview (Swagger at `/swagger-ui.html`), external credentials checklist, final FLEETPULSE_PROGRESS status
4. Final static review pass for compile errors (backend + web) since nothing has been built

Type CONTINUE to start the desktop and mobile apps.
