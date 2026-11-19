import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { api, errorMessage } from '../../api.js';
import { Badge, Button, EmptyState, ErrorNote, PageHeader, Spinner, StatusBadge, fmtDate, fmtMoney, useToast } from '../../ui.jsx';

function Meter({ label, used, limit }) {
  const pct = limit ? Math.min(100, Math.round((Number(used) / Number(limit)) * 100)) : 0;
  return (<div className="fp-card fp-card--padded"><p className="text-xs uppercase tracking-wide text-fp-muted">{label}</p><p className="mt-2 text-xl font-semibold">{Number(used).toLocaleString()} <span className="text-sm font-normal text-fp-muted">/ {Number(limit).toLocaleString()}</span></p>
    <div className="mt-3 h-1.5 rounded bg-fp-inset" role="progressbar" aria-valuenow={pct} aria-valuemin={0} aria-valuemax={100} aria-label={label}><div className="h-1.5 rounded" style={{ width: `${pct}%`, background: pct > 90 ? 'var(--fp-error)' : 'var(--fp-orange)' }} /></div></div>);
}

export default function Subscription() {
  const toast = useToast();
  const [params] = useSearchParams();
  const [s, setS] = useState(null);
  const [plans, setPlans] = useState([]);
  const [billing, setBilling] = useState(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState('');
  const load = () => api.get('/company/subscription').then((r) => setS(r.data)).catch((e) => setError(errorMessage(e)));
  useEffect(() => { load(); api.get('/public/plans').then((r) => setPlans(r.data)); api.get('/billing/status').then((r) => setBilling(r.data)).catch(() => {}); }, []);
  useEffect(() => { if (params.get('checkout') === 'success') toast('Payment received. Your subscription will update shortly.'); }, [params, toast]);

  async function checkout(planCode, billingCycle) {
    setBusy(planCode + billingCycle); setError('');
    try { const { data } = await api.post('/billing/checkout', { planCode, billingCycle }); window.location.assign(data.checkoutUrl); }
    catch (e) { setError(errorMessage(e)); setBusy(''); }
  }
  async function cancel() {
    if (!window.confirm('Cancel your subscription at the end of the current period?')) return;
    try { await api.post('/company/subscription/cancel'); toast('Cancellation scheduled'); load(); } catch (e) { setError(errorMessage(e)); }
  }

  if (!s) return error ? <ErrorNote>{error}</ErrorNote> : <Spinner />;
  const p = s.plan;
  return (
    <>
      <PageHeader title="Subscription" subtitle="Plan, usage, billing and invoices" actions={<StatusBadge value={p.status} />} />
      <ErrorNote>{error}</ErrorNote>
      <div className="fp-card fp-card--padded mb-4">
        <p className="text-lg font-semibold">{p.name} plan</p>
        <p className="mt-1 text-sm text-fp-muted">{p.status === 'TRIALING' ? `Trial ends ${fmtDate(p.trial_ends_at)}` : `Current period ends ${fmtDate(p.current_period_end)}`}</p>
        {p.status !== 'CANCELLED' && <Button variant="ghost" className="mt-2" onClick={cancel}>Cancel subscription</Button>}
      </div>
      <div className="mb-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Meter label="Vehicles" used={s.usage.vehicles} limit={p.vehicle_limit} /><Meter label="Users" used={s.usage.users} limit={p.user_limit} />
        <Meter label="AI requests this month" used={s.usage.aiRequestsThisMonth} limit={p.ai_monthly_quota} /><Meter label="GPS points this month" used={s.usage.gpsPointsThisMonth} limit={p.gps_monthly_quota} />
      </div>
      <h2 className="mb-2 font-medium">Change plan</h2>
      {billing && !billing.paymentsConfigured && <p className="mb-3 text-sm text-fp-muted">Online payment is not enabled on this server yet, so plan changes cannot be completed here. Contact support.</p>}
      <div className="grid gap-4 md:grid-cols-3">
        {plans.map((pl) => (
          <div key={pl.code} className="fp-card fp-card--padded"><div className="flex justify-between"><p className="font-medium">{pl.name}</p>{pl.code === p.code && <Badge>Current</Badge>}</div>
            <p className="mt-1 text-sm text-fp-muted">{pl.vehicleLimit} vehicles · {pl.userLimit} users</p>
            <div className="mt-3 flex flex-wrap gap-2"><Button variant="secondary" disabled={!billing?.paymentsConfigured} loading={busy === pl.code + 'MONTHLY'} onClick={() => checkout(pl.code, 'MONTHLY')}>{fmtMoney(pl.monthlyPrice, pl.currency)} / mo</Button><Button variant="secondary" disabled={!billing?.paymentsConfigured} loading={busy === pl.code + 'ANNUAL'} onClick={() => checkout(pl.code, 'ANNUAL')}>{fmtMoney(pl.annualPrice, pl.currency)} / yr</Button></div></div>))}
      </div>
      <h2 className="mb-2 mt-8 font-medium">Invoices and payment history</h2>
      <div className="fp-card overflow-x-auto">{s.invoices.length === 0 ? <EmptyState title="No invoices yet" /> : (
        <table className="fp-table"><thead><tr><th>Invoice</th><th>Issued</th><th>Total</th><th>Status</th></tr></thead><tbody>{s.invoices.map((i) => <tr key={i.invoiceNumber}><td>{i.invoiceNumber}</td><td>{fmtDate(i.issuedAt)}</td><td>{fmtMoney(i.total, i.currency)}</td><td><StatusBadge value={i.status} /></td></tr>)}</tbody></table>)}</div>
    </>
  );
}
