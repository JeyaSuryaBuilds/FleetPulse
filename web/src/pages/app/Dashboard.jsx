import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { api, errorMessage } from '../../api.js';
import { useAuth } from '../../auth.jsx';
import { EmptyState, ErrorNote, PageHeader, Spinner, Stat, StatusBadge, fmtDate, fmtDateTime, fmtMoney } from '../../ui.jsx';
import MyWork from './MyWork.jsx';

const axis = { stroke: '#A3A3A3', fontSize: 12 };

export default function Dashboard() {
  const { can, user } = useAuth();
  const [d, setD] = useState(null);
  const [error, setError] = useState('');
  const allowed = can('DASHBOARD_VIEW');
  useEffect(() => { if (allowed) api.get('/dashboard').then((r) => setD(r.data)).catch((e) => setError(errorMessage(e))); }, [allowed]);

  if (!allowed) return (<><PageHeader title={`Welcome, ${user.fullName}`} subtitle="Your assignments and pay" /><MyWork /></>);
  if (error) return <ErrorNote>{error}</ErrorNote>;
  if (!d) return <Spinner />;
  const profit = Number(d.revenueMonthToDate) - Number(d.expensesMonthToDate);

  return (
    <>
      <PageHeader title="Dashboard" subtitle="Live figures from your company data" />
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Stat label="Total vehicles" value={d.totalVehicles} /><Stat label="Active vehicles" value={d.activeVehicles} /><Stat label="Available" value={d.availableVehicles} /><Stat label="In maintenance" value={d.maintenanceVehicles} />
        <Stat label="Drivers" value={d.totalDrivers} /><Stat label="Employees" value={d.totalEmployees} /><Stat label="Active shipments" value={d.activeShipments} /><Stat label="Pending shipments" value={d.pendingShipments} />
        <Stat label="Delivered" value={d.deliveredShipments} tone="success" /><Stat label="Delayed" value={d.delayedShipments} tone={d.delayedShipments > 0 ? 'error' : undefined} />
        <Stat label="Revenue (month to date)" value={fmtMoney(d.revenueMonthToDate)} /><Stat label="Expenses (month to date)" value={fmtMoney(d.expensesMonthToDate)} sub={`Net ${fmtMoney(profit)}`} />
        <Stat label="Fuel (month to date)" value={fmtMoney(d.fuelExpensesMonthToDate)} /><Stat label="Maintenance (month to date)" value={fmtMoney(d.maintenanceExpensesMonthToDate)} />
      </div>

      <div className="mt-6 grid gap-4 lg:grid-cols-2">
        <div className="fp-card fp-card--padded"><h2 className="mb-3 font-medium">Shipments created, last 30 days</h2>
          {d.shipmentsPerDay.length === 0 ? <EmptyState title="No shipments in this period" /> : (
            <div style={{ height: 240 }}><ResponsiveContainer><BarChart data={d.shipmentsPerDay.map((x) => ({ ...x, day: String(x.day).slice(5) }))}><CartesianGrid stroke="#303030" vertical={false} /><XAxis dataKey="day" {...axis} /><YAxis allowDecimals={false} {...axis} /><Tooltip contentStyle={{ background: '#1D1D1D', border: '1px solid #303030' }} /><Bar dataKey="count" fill="#FF8733" radius={[3, 3, 0, 0]} /></BarChart></ResponsiveContainer></div>)}
        </div>
        <div className="fp-card fp-card--padded"><h2 className="mb-3 font-medium">Shipments by status</h2>
          {d.shipmentsByStatus.length === 0 ? <EmptyState title="No shipments yet" /> : (
            <div style={{ height: 240 }}><ResponsiveContainer><BarChart layout="vertical" data={d.shipmentsByStatus.map((x) => ({ ...x, status: x.status.replaceAll('_', ' ') }))}><CartesianGrid stroke="#303030" horizontal={false} /><XAxis type="number" allowDecimals={false} {...axis} /><YAxis type="category" dataKey="status" width={130} {...axis} /><Tooltip contentStyle={{ background: '#1D1D1D', border: '1px solid #303030' }} /><Bar dataKey="count" fill="#F97316" radius={[0, 3, 3, 0]} /></BarChart></ResponsiveContainer></div>)}
        </div>
      </div>

      <div className="mt-6 grid gap-4 lg:grid-cols-3">
        <div className="fp-card fp-card--padded"><h2 className="mb-3 font-medium">Expiring vehicle documents</h2>
          {d.expiringDocuments.length === 0 ? <p className="text-sm text-fp-muted">Nothing expires in the next 30 days.</p> : <ul className="space-y-2 text-sm">{d.expiringDocuments.map((x, i) => <li key={i} className="flex justify-between"><span>{x.docType}</span><span className="text-fp-muted">{fmtDate(x.expiryDate)}</span></li>)}</ul>}
          <Link to="/app/vehicle-documents" className="mt-3 inline-block text-sm text-orange underline">Open documents</Link></div>
        <div className="fp-card fp-card--padded"><h2 className="mb-3 font-medium">Licences expiring</h2>
          {d.expiringLicences.length === 0 ? <p className="text-sm text-fp-muted">No driver licences expire in the next 30 days.</p> : <ul className="space-y-2 text-sm">{d.expiringLicences.map((x) => <li key={x.driverId} className="flex justify-between"><span>{x.fullName}</span><span className="text-fp-muted">{fmtDate(x.licenceExpiry)}</span></li>)}</ul>}</div>
        <div className="fp-card fp-card--padded"><h2 className="mb-3 font-medium">Recent activity</h2>
          {d.recentActivity.length === 0 ? <p className="text-sm text-fp-muted">No activity yet.</p> : <ul className="space-y-2 text-sm">{d.recentActivity.slice(0, 8).map((x, i) => <li key={i} className="flex justify-between gap-2"><span className="truncate">{x.action.replaceAll('_', ' ').toLowerCase()}</span><span className="shrink-0 text-xs text-fp-muted">{fmtDateTime(x.createdAt)}</span></li>)}</ul>}</div>
      </div>
    </>
  );
}
