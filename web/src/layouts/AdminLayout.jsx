import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { LogOut } from 'lucide-react';
import { FleetPulseLogo } from '../components/brand';
import { adminSession, api } from '../api.js';

const items = [['dashboard', 'Dashboard'], ['companies', 'Companies'], ['plans', 'Plans and pricing'], ['finance', 'Finance'], ['ai', 'AI management'], ['platform', 'Platform'], ['success', 'Customer success'], ['security', 'Security'], ['settings', 'Configuration']];

export default function AdminLayout() {
  const nav = useNavigate();
  return (
    <div className="min-h-screen bg-fp-bg text-fp-text lg:grid lg:grid-cols-[240px_1fr]">
      <aside className="border-b border-fp-border bg-fp-sidebar lg:sticky lg:top-0 lg:h-screen lg:border-b-0 lg:border-r">
        <div className="px-5 py-5"><FleetPulseLogo variant="horizontal" height={28} decorative /><p className="mt-2 text-xs text-fp-muted">Fayzo Technologies Super Admin</p></div>
        <nav className="flex gap-1 overflow-x-auto px-3 pb-3 lg:flex-col lg:overflow-visible" aria-label="Super Admin">
          {items.map(([to, l]) => <NavLink key={to} to={`/admin/${to}`} className={({ isActive }) => `fp-nav-link whitespace-nowrap ${isActive ? 'is-active' : ''}`}>{l}</NavLink>)}
        </nav>
        <div className="hidden px-3 lg:block"><button className="fp-nav-link flex w-full items-center gap-2" onClick={() => { adminSession.clear(); nav('/admin'); }}><LogOut size={16} aria-hidden /> Sign out</button></div>
      </aside>
      <main className="min-w-0 px-4 py-8 sm:px-8"><Outlet />
        <button className="fp-btn fp-btn--ghost mt-8 lg:hidden" onClick={() => { adminSession.clear(); nav('/admin'); }}>Sign out</button></main>
    </div>
  );
}
