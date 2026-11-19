import { Suspense, lazy } from 'react';
import { BrowserRouter, Link, Navigate, Route, Routes } from 'react-router-dom';
import { AuthProvider, RequireAdmin, RequireAuth } from './auth.jsx';
import { Spinner, ToastProvider } from './ui.jsx';
import { resources } from './resources.js';
import PublicLayout from './layouts/PublicLayout.jsx';
import Home from './pages/public/Home.jsx';
import Pricing from './pages/public/Pricing.jsx';
import { Features, Solutions, About } from './pages/public/Static.jsx';
import Contact from './pages/public/Contact.jsx';
import Login from './pages/public/Login.jsx';
import Register from './pages/public/Register.jsx';
import Track from './pages/public/Track.jsx';
import AdminLogin from './pages/admin/AdminLogin.jsx';

const AppLayout = lazy(() => import('./layouts/AppLayout.jsx'));
const AdminLayout = lazy(() => import('./layouts/AdminLayout.jsx'));
const ResourcePage = lazy(() => import('./pages/app/ResourcePage.jsx'));
const page = (name) => lazy(() => import(`./pages/app/${name}.jsx`));
const adminPage = (name) => lazy(() => import(`./pages/admin/${name}.jsx`));

const Dashboard = page('Dashboard'), MyWork = page('MyWork'), LiveMap = page('LiveMap'), Notifications = page('Notifications'), Ai = page('Ai'), Reports = page('Reports');
const Analytics = page('Analytics'), RoutePlanner = page('RoutePlanner'), PayrollRuns = page('PayrollRuns'), Company = page('Company'), Users = page('Users'), Subscription = page('Subscription');
const AdminDashboard = adminPage('AdminDashboard'), AdminCompanies = adminPage('AdminCompanies'), AdminPlans = adminPage('AdminPlans'), AdminFinance = adminPage('AdminFinance');
const AdminAi = adminPage('AdminAi'), AdminPlatform = adminPage('AdminPlatform'), AdminSuccess = adminPage('AdminSuccess'), AdminSecurity = adminPage('AdminSecurity'), AdminSettings = adminPage('AdminSettings');

const NotFound = () => (
  <div className="mx-auto max-w-md px-4 py-24 text-center"><h1 className="text-3xl font-semibold">Page not found</h1><p className="mt-2 text-fp-muted">The page you are looking for does not exist.</p><Link to="/" className="fp-btn fp-btn--primary mt-6">Go home</Link></div>
);

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <ToastProvider>
          <Suspense fallback={<Spinner />}>
            <Routes>
              <Route element={<PublicLayout />}>
                <Route index element={<Home />} />
                <Route path="features" element={<Features />} />
                <Route path="solutions" element={<Solutions />} />
                <Route path="pricing" element={<Pricing />} />
                <Route path="about" element={<About />} />
                <Route path="contact" element={<Contact />} />
                <Route path="login" element={<Login />} />
                <Route path="register" element={<Register />} />
                <Route path="track" element={<Track />} />
                <Route path="track/:id" element={<Track />} />
                <Route path="*" element={<NotFound />} />
              </Route>

              <Route path="/admin">
                <Route index element={<AdminLogin />} />
                <Route element={<RequireAdmin><AdminLayout /></RequireAdmin>}>
                  <Route path="dashboard" element={<AdminDashboard />} />
                  <Route path="companies" element={<AdminCompanies />} />
                  <Route path="plans" element={<AdminPlans />} />
                  <Route path="finance" element={<AdminFinance />} />
                  <Route path="ai" element={<AdminAi />} />
                  <Route path="platform" element={<AdminPlatform />} />
                  <Route path="success" element={<AdminSuccess />} />
                  <Route path="security" element={<AdminSecurity />} />
                  <Route path="settings" element={<AdminSettings />} />
                </Route>
              </Route>

              <Route path="/app" element={<RequireAuth><AppLayout /></RequireAuth>}>
                <Route index element={<Dashboard />} />
                <Route path="me" element={<MyWork standalone />} />
                <Route path="live-map" element={<RequireAuth perm="GPS_READ"><LiveMap /></RequireAuth>} />
                <Route path="notifications" element={<RequireAuth perm="NOTIFICATION_READ"><Notifications /></RequireAuth>} />
                <Route path="ai" element={<RequireAuth perm="AI_USE"><Ai /></RequireAuth>} />
                <Route path="reports" element={<RequireAuth perm="REPORT_VIEW"><Reports /></RequireAuth>} />
                <Route path="analytics" element={<RequireAuth perm="FLEET_READ"><Analytics /></RequireAuth>} />
                <Route path="route-planner" element={<RequireAuth perm="ROUTE_READ"><RoutePlanner /></RequireAuth>} />
                <Route path="payroll-runs" element={<RequireAuth perm="PAYROLL_READ"><PayrollRuns /></RequireAuth>} />
                <Route path="company" element={<RequireAuth perm="DASHBOARD_VIEW"><Company /></RequireAuth>} />
                <Route path="users" element={<RequireAuth perm="USER_MANAGE"><Users /></RequireAuth>} />
                <Route path="subscription" element={<RequireAuth perm="SUBSCRIPTION_MANAGE"><Subscription /></RequireAuth>} />
                {resources.map((r) => <Route key={r.path} path={r.path} element={<RequireAuth perm={r.read}><ResourcePage key={r.path} config={r} /></RequireAuth>} />)}
                <Route path="*" element={<Navigate to="/app" replace />} />
              </Route>
            </Routes>
          </Suspense>
        </ToastProvider>
      </AuthProvider>
    </BrowserRouter>
  );
}
