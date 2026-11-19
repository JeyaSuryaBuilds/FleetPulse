import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { ErrorNote, PageHeader, Spinner, Stat, fmtMoney } from '../../ui.jsx';
import { useGet } from './adminUtil.js';

export default function AdminDashboard() {
  const { data: d, error } = useGet('/admin/dashboard');
  if (error) return <ErrorNote>{error}</ErrorNote>;
  if (!d) return <Spinner />;
  const axis = { stroke: '#A3A3A3', fontSize: 12 };
  return (
    <>
      <PageHeader title="Platform dashboard" subtitle="MRR counts only paying (active) subscriptions; trials are excluded." />
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Stat label="Total companies" value={d.totalCompanies} /><Stat label="Active" value={d.activeCompanies} tone="success" /><Stat label="On trial" value={d.trialCompanies} /><Stat label="Suspended" value={d.suspendedCompanies} tone={d.suspendedCompanies ? 'error' : undefined} />
        <Stat label="Expired" value={d.expiredCompanies} /><Stat label="Cancelled" value={d.cancelledCompanies} /><Stat label="Total users" value={d.totalUsers} /><Stat label="Total vehicles" value={d.totalVehicles} />
        <Stat label="MRR" value={fmtMoney(d.mrr)} /><Stat label="ARR" value={fmtMoney(d.arr)} /><Stat label="Subscription revenue (all time)" value={fmtMoney(d.subscriptionRevenueTotal)} sub={`This month ${fmtMoney(d.subscriptionRevenueThisMonth)}`} />
        <Stat label="AI requests (month)" value={d.aiRequestsThisMonth} sub={`${Number(d.aiTokensThisMonth).toLocaleString()} tokens`} />
      </div>
      <div className="mt-6 grid gap-4 lg:grid-cols-2">
        <div className="fp-card fp-card--padded"><h2 className="mb-3 font-medium">Sign-ups, last 30 days</h2>
          {d.signupsPerDay.length === 0 ? <p className="text-sm text-fp-muted">No sign-ups in this period.</p> : <div style={{ height: 220 }}><ResponsiveContainer><BarChart data={d.signupsPerDay.map((x) => ({ ...x, day: String(x.day).slice(5) }))}><CartesianGrid stroke="#303030" vertical={false} /><XAxis dataKey="day" {...axis} /><YAxis allowDecimals={false} {...axis} /><Tooltip contentStyle={{ background: '#1D1D1D', border: '1px solid #303030' }} /><Bar dataKey="count" fill="#FF8733" radius={[3, 3, 0, 0]} /></BarChart></ResponsiveContainer></div>}</div>
        <div className="fp-card fp-card--padded"><h2 className="mb-3 font-medium">Companies by plan</h2>
          {d.companiesByPlan.length === 0 ? <p className="text-sm text-fp-muted">No subscriptions yet.</p> : <ul className="space-y-2 text-sm">{d.companiesByPlan.map((x) => <li key={x.plan} className="flex justify-between"><span>{x.plan}</span><span className="text-fp-muted">{x.count}</span></li>)}</ul>}</div>
      </div>
    </>
  );
}
