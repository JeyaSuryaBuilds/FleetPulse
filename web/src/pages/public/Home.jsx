import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { ChevronDown, Check } from 'lucide-react';
import { FleetPulseLogo } from '../../components/brand';
import { api } from '../../api.js';
import { cardEntrance, listContainer } from '../../design-system/motion.js';
import { modules, benefits, faqs, businessTypes } from './content.js';
import { PlanCards } from './Pricing.jsx';

/** Hero image slot. Drop a photographic fleet render at web/public/images/hero-fleet.jpg; without it a neutral brand panel is shown. */
function HeroVisual() {
  const [ok, setOk] = useState(true);
  return (
    <div className="fp-card relative overflow-hidden" style={{ aspectRatio: '16 / 10' }}>
      {ok ? (
        <img src="/images/hero-fleet.jpg" alt="Fleet of delivery trucks on the road" className="h-full w-full object-cover" onError={() => setOk(false)} />
      ) : (
        <div className="flex h-full w-full items-center justify-center bg-fp-inset">
          <FleetPulseLogo variant="icon" height={96} decorative />
        </div>
      )}
      <div className="pointer-events-none absolute inset-0 bg-gradient-to-t from-fp-bg/70 via-transparent to-transparent" />
    </div>
  );
}

function DashboardPreview() {
  const kpis = [['Vehicles', '—'], ['Active shipments', '—'], ['Delayed', '—'], ['Revenue', '—']];
  return (
    <div className="fp-card fp-card--padded">
      <div className="mb-4 flex items-center justify-between"><p className="font-medium">Company dashboard</p><span className="fp-badge fp-badge--brand">Product preview</span></div>
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
        {kpis.map(([l, v]) => <div key={l} className="rounded-fp-md border border-fp-border bg-fp-inset p-3"><p className="text-xs text-fp-muted">{l}</p><p className="mt-1 text-xl font-semibold">{v}</p></div>)}
      </div>
      <svg viewBox="0 0 600 140" className="mt-4 w-full" role="img" aria-label="Illustrative chart outline">
        <defs><linearGradient id="g" x1="0" x2="0" y1="0" y2="1"><stop offset="0" stopColor="#FF8733" stopOpacity="0.28" /><stop offset="1" stopColor="#FF8733" stopOpacity="0" /></linearGradient></defs>
        {[30, 60, 90, 120].map((y) => <line key={y} x1="0" x2="600" y1={y} y2={y} stroke="#303030" strokeWidth="1" />)}
        <path d="M0 110 L60 96 L120 102 L180 70 L240 80 L300 52 L360 60 L420 34 L480 44 L540 24 L600 30 L600 140 L0 140Z" fill="url(#g)" />
        <path d="M0 110 L60 96 L120 102 L180 70 L240 80 L300 52 L360 60 L420 34 L480 44 L540 24 L600 30" fill="none" stroke="#FF8733" strokeWidth="2" />
      </svg>
      <p className="mt-2 text-xs text-fp-muted">Illustration of the dashboard layout. Your dashboard shows your own live figures after sign-up.</p>
    </div>
  );
}

export default function Home() {
  const [plans, setPlans] = useState(null);
  const [open, setOpen] = useState(0);
  useEffect(() => { api.get('/public/plans').then((r) => setPlans(r.data)).catch(() => setPlans([])); }, []);

  return (
    <>
      <section className="mx-auto grid max-w-7xl items-center gap-10 px-4 pb-16 pt-14 sm:px-6 lg:grid-cols-2 lg:pt-20">
        <motion.div {...cardEntrance}>
          <span className="fp-badge fp-badge--brand">Smart Fleet &amp; Logistics Management</span>
          <h1 className="mt-5 text-4xl font-semibold leading-tight tracking-tight sm:text-5xl">Run your whole fleet from one platform.</h1>
          <p className="mt-5 max-w-xl text-lg text-fp-muted">FleetPulse brings vehicles, drivers, shipments, live tracking, payroll and finance together, on web, Windows, Android and iPhone.</p>
          <div className="mt-8 flex flex-wrap gap-3">
            <Link to="/register" className="fp-btn fp-btn--primary">Get started</Link>
            <Link to="/contact?type=demo" className="fp-btn fp-btn--secondary">Request a demo</Link>
          </div>
          <p className="mt-4 text-sm text-fp-muted">Start with a free trial. No manual approval.</p>
        </motion.div>
        <motion.div {...cardEntrance}><HeroVisual /></motion.div>
      </section>

      <section className="mx-auto max-w-7xl px-4 sm:px-6"><DashboardPreview /></section>

      <section className="mx-auto mt-24 max-w-7xl px-4 sm:px-6">
        <h2 className="text-3xl font-semibold tracking-tight">Everything your operation needs</h2>
        <p className="mt-2 max-w-2xl text-fp-muted">Sixteen connected modules, one login.</p>
        <motion.div className="mt-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-4" variants={listContainer} initial="initial" whileInView="animate" viewport={{ once: true }}>
          {modules.slice(0, 8).map(({ icon: Icon, title, text }) => (
            <motion.div key={title} variants={cardEntrance} className="fp-card fp-card--padded">
              <Icon size={22} className="text-orange" aria-hidden />
              <h3 className="mt-3 font-medium">{title}</h3>
              <p className="mt-1.5 text-sm text-fp-muted">{text}</p>
            </motion.div>
          ))}
        </motion.div>
        <div className="mt-6"><Link to="/features" className="fp-btn fp-btn--ghost">See all features</Link></div>
      </section>

      <section className="mx-auto mt-24 max-w-7xl px-4 sm:px-6">
        <h2 className="text-3xl font-semibold tracking-tight">Why teams choose FleetPulse</h2>
        <div className="mt-8 grid gap-4 md:grid-cols-2">
          {benefits.map(([t, d]) => (
            <div key={t} className="fp-card fp-card--padded flex gap-3"><Check className="mt-0.5 shrink-0 text-success" size={20} aria-hidden /><div><h3 className="font-medium">{t}</h3><p className="mt-1 text-sm text-fp-muted">{d}</p></div></div>
          ))}
        </div>
      </section>

      <section className="mx-auto mt-24 max-w-7xl px-4 sm:px-6">
        <h2 className="text-3xl font-semibold tracking-tight">Built for</h2>
        <div className="mt-6 flex flex-wrap gap-2">{businessTypes.map((b) => <span key={b} className="fp-badge fp-badge--brand">{b}</span>)}</div>
      </section>

      <section className="mx-auto mt-24 max-w-7xl px-4 sm:px-6">
        <div className="flex items-end justify-between"><h2 className="text-3xl font-semibold tracking-tight">Simple, transparent pricing</h2><Link to="/pricing" className="fp-btn fp-btn--ghost">Compare plans</Link></div>
        <div className="mt-8"><PlanCards plans={plans} compact /></div>
      </section>

      <section className="mx-auto mt-24 max-w-3xl px-4 sm:px-6">
        <h2 className="text-3xl font-semibold tracking-tight">Frequently asked questions</h2>
        <div className="mt-6 divide-y divide-fp-border rounded-fp-lg border border-fp-border bg-fp-card">
          {faqs.map(([q, a], i) => (
            <div key={q}>
              <button className="flex w-full items-center justify-between px-5 py-4 text-left font-medium" aria-expanded={open === i} onClick={() => setOpen(open === i ? -1 : i)}>
                {q}<ChevronDown size={18} className={`transition ${open === i ? 'rotate-180' : ''}`} aria-hidden />
              </button>
              {open === i && <p className="px-5 pb-5 text-sm text-fp-muted">{a}</p>}
            </div>
          ))}
        </div>
      </section>

      <section className="mx-auto mt-24 max-w-7xl px-4 sm:px-6">
        <div className="fp-card fp-card--padded flex flex-wrap items-center justify-between gap-4 p-8">
          <div><h2 className="text-2xl font-semibold">Ready to see your fleet clearly?</h2><p className="mt-1 text-fp-muted">Create your workspace in minutes.</p></div>
          <div className="flex gap-3"><Link to="/register" className="fp-btn fp-btn--primary">Get started</Link><Link to="/contact?type=demo" className="fp-btn fp-btn--secondary">Request a demo</Link></div>
        </div>
      </section>
    </>
  );
}
