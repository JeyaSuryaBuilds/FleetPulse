import { Link } from 'react-router-dom';
import { modules, solutions } from './content.js';

export function Features() {
  return (
    <section className="mx-auto max-w-7xl px-4 py-16 sm:px-6">
      <h1 className="text-4xl font-semibold tracking-tight">Features</h1>
      <p className="mt-3 max-w-2xl text-fp-muted">Every module shares the same data, so a delivery updates the shipment, the driver’s earnings, the customer’s tracking page and your reports at once.</p>
      <div className="mt-10 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {modules.map(({ icon: Icon, title, text }) => (
          <div key={title} className="fp-card fp-card--padded"><Icon size={22} className="text-orange" aria-hidden /><h2 className="mt-3 font-medium">{title}</h2><p className="mt-1.5 text-sm text-fp-muted">{text}</p></div>
        ))}
      </div>
    </section>
  );
}

export function Solutions() {
  return (
    <section className="mx-auto max-w-7xl px-4 py-16 sm:px-6">
      <h1 className="text-4xl font-semibold tracking-tight">Solutions</h1>
      <p className="mt-3 max-w-2xl text-fp-muted">FleetPulse adapts to how different transport and logistics businesses work.</p>
      <div className="mt-10 grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        {solutions.map((s) => <div key={s.title} className="fp-card fp-card--padded"><h2 className="font-medium">{s.title}</h2><p className="mt-2 text-sm text-fp-muted">{s.text}</p></div>)}
      </div>
      <div className="mt-10"><Link to="/register" className="fp-btn fp-btn--primary">Start free trial</Link></div>
    </section>
  );
}

export function About() {
  return (
    <section className="mx-auto max-w-3xl px-4 py-16 sm:px-6">
      <h1 className="text-4xl font-semibold tracking-tight">About FleetPulse</h1>
      <p className="mt-5 text-fp-muted">FleetPulse is a Smart Fleet &amp; Logistics Management System developed and owned by Fayzo Technologies. It gives transport, courier and logistics businesses one connected platform for vehicles, drivers, shipments, tracking, payroll and finance.</p>
      <p className="mt-4 text-fp-muted">We build around a few principles: show only real data, keep every company’s information strictly separate, and make daily work fast on whatever device people have in hand.</p>
      <div className="mt-8 flex gap-3"><Link to="/contact" className="fp-btn fp-btn--secondary">Contact us</Link><Link to="/register" className="fp-btn fp-btn--primary">Get started</Link></div>
    </section>
  );
}
