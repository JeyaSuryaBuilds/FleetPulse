import { useState } from 'react';
import { Link, NavLink, Outlet } from 'react-router-dom';
import { Menu, X } from 'lucide-react';
import { FleetPulseLogo } from '../components/brand';
import { useAuth } from '../auth.jsx';

const links = [['/features', 'Features'], ['/solutions', 'Solutions'], ['/pricing', 'Pricing'], ['/about', 'About'], ['/contact', 'Contact'], ['/track', 'Track shipment']];

export default function PublicLayout() {
  const [open, setOpen] = useState(false);
  const { user } = useAuth();
  return (
    <div className="min-h-screen bg-fp-bg text-fp-text">
      <header className="sticky top-0 z-40 border-b border-fp-border bg-fp-bg/90 backdrop-blur">
        <div className="mx-auto flex h-16 max-w-7xl items-center justify-between px-4 sm:px-6">
          <Link to="/" aria-label="FleetPulse home"><FleetPulseLogo variant="horizontal" height={30} decorative /></Link>
          <nav className="hidden items-center gap-1 md:flex" aria-label="Main">
            {links.map(([to, label]) => <NavLink key={to} to={to} className="fp-nav-link">{label}</NavLink>)}
          </nav>
          <div className="hidden items-center gap-2 md:flex">
            {user ? <Link to="/app" className="fp-btn fp-btn--primary">Open dashboard</Link> : (<>
              <Link to="/login" className="fp-btn fp-btn--ghost">Log in</Link>
              <Link to="/register" className="fp-btn fp-btn--primary">Get started</Link>
            </>)}
          </div>
          <button className="fp-btn fp-btn--ghost md:hidden" onClick={() => setOpen(!open)} aria-label="Menu" aria-expanded={open}>{open ? <X /> : <Menu />}</button>
        </div>
        {open && (
          <div className="border-t border-fp-border px-4 pb-4 md:hidden">
            {links.map(([to, label]) => <NavLink key={to} to={to} onClick={() => setOpen(false)} className="fp-nav-link my-1 block">{label}</NavLink>)}
            <div className="mt-3 flex gap-2">
              <Link to="/login" className="fp-btn fp-btn--secondary flex-1" onClick={() => setOpen(false)}>Log in</Link>
              <Link to="/register" className="fp-btn fp-btn--primary flex-1" onClick={() => setOpen(false)}>Get started</Link>
            </div>
          </div>
        )}
      </header>
      <main><Outlet /></main>
      <footer className="mt-24 border-t border-fp-border bg-fp-sidebar">
        <div className="mx-auto grid max-w-7xl gap-8 px-4 py-12 sm:px-6 md:grid-cols-4">
          <div className="md:col-span-2">
            <FleetPulseLogo variant="horizontal" height={30} decorative />
            <p className="mt-4 max-w-sm text-sm text-fp-muted">Smart Fleet &amp; Logistics Management. One platform for vehicles, drivers, shipments, live tracking, payroll and finance.</p>
          </div>
          <div><p className="mb-3 text-sm font-medium">Product</p>
            <ul className="space-y-2 text-sm text-fp-muted">{links.slice(0, 3).map(([to, l]) => <li key={to}><Link className="hover:text-fp-text" to={to}>{l}</Link></li>)}<li><Link className="hover:text-fp-text" to="/track">Track shipment</Link></li></ul></div>
          <div><p className="mb-3 text-sm font-medium">Company</p>
            <ul className="space-y-2 text-sm text-fp-muted"><li><Link className="hover:text-fp-text" to="/about">About</Link></li><li><Link className="hover:text-fp-text" to="/contact">Contact</Link></li><li><Link className="hover:text-fp-text" to="/contact?type=demo">Request a demo</Link></li><li><Link className="hover:text-fp-text" to="/login">Log in</Link></li></ul></div>
        </div>
        <div className="border-t border-fp-border py-4 text-center text-xs text-fp-muted">© {new Date().getFullYear()} Fayzo Technologies. FleetPulse is a product of Fayzo Technologies.</div>
      </footer>
    </div>
  );
}
