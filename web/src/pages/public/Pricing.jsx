import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Check } from 'lucide-react';
import { api } from '../../api.js';
import { Spinner, fmtMoney } from '../../ui.jsx';

export function PlanCards({ plans, compact = false, annual = false }) {
  if (plans === null) return <Spinner />;
  if (!plans.length) return <p className="text-fp-muted">Pricing is currently unavailable. Please check back shortly.</p>;
  return (
    <div className="grid gap-4 md:grid-cols-3">
      {plans.map((p, i) => {
        const price = annual ? p.annualPrice : p.monthlyPrice;
        const featured = plans.length === 3 && i === 1;
        return (
          <div key={p.code} className={`fp-card fp-card--padded flex flex-col ${featured ? 'border-orange' : ''}`}>
            <div className="flex items-center justify-between"><h3 className="text-lg font-semibold">{p.name}</h3>{featured && <span className="fp-badge fp-badge--brand">Popular</span>}</div>
            <p className="mt-1 min-h-[2.5rem] text-sm text-fp-muted">{p.description}</p>
            <p className="mt-4"><span className="text-3xl font-semibold">{fmtMoney(price, p.currency)}</span><span className="text-sm text-fp-muted"> / {annual ? 'year' : 'month'}</span></p>
            <ul className="mt-5 space-y-2 text-sm">
              {[`${p.trialDays}-day free trial`, `Up to ${p.vehicleLimit.toLocaleString()} vehicles`, `Up to ${p.userLimit.toLocaleString()} users`, `${p.aiMonthlyQuota.toLocaleString()} AI requests / month`, `${p.gpsMonthlyQuota.toLocaleString()} GPS points / month`]
                .slice(0, compact ? 4 : 5).map((t) => <li key={t} className="flex gap-2"><Check size={16} className="mt-0.5 shrink-0 text-success" aria-hidden />{t}</li>)}
            </ul>
            <Link to={`/register?plan=${p.code}`} className={`fp-btn mt-6 ${featured ? 'fp-btn--primary' : 'fp-btn--secondary'}`}>Start free trial</Link>
          </div>
        );
      })}
    </div>
  );
}

export default function Pricing() {
  const [plans, setPlans] = useState(null);
  const [annual, setAnnual] = useState(false);
  useEffect(() => { api.get('/public/plans').then((r) => setPlans(r.data)).catch(() => setPlans([])); }, []);
  return (
    <section className="mx-auto max-w-7xl px-4 py-16 sm:px-6">
      <h1 className="text-4xl font-semibold tracking-tight">Pricing</h1>
      <p className="mt-3 max-w-2xl text-fp-muted">Every plan starts with a free trial. Plans, limits and prices are managed by the FleetPulse team and shown here live.</p>
      <div className="mt-6 inline-flex rounded-fp-md border border-fp-border p-1" role="group" aria-label="Billing period">
        {[['Monthly', false], ['Annual', true]].map(([l, v]) => (
          <button key={l} onClick={() => setAnnual(v)} aria-pressed={annual === v} className={`rounded-fp-sm px-4 py-1.5 text-sm ${annual === v ? 'bg-orange text-fp-on-orange font-medium' : 'text-fp-muted'}`}>{l}</button>
        ))}
      </div>
      <div className="mt-8"><PlanCards plans={plans} annual={annual} /></div>
      <p className="mt-6 text-sm text-fp-muted">Need more than the Enterprise limits or a custom setup? <Link to="/contact" className="text-orange underline">Talk to us</Link>.</p>
    </section>
  );
}
