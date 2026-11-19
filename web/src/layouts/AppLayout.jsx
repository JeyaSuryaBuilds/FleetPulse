import { useMemo, useState } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { LogOut, Menu, X, ChevronDown } from 'lucide-react';
import { FleetPulseLogo } from '../components/brand';
import { useAuth } from '../auth.jsx';
import { buildNav } from '../nav.js';
import { Badge } from '../ui.jsx';

export default function AppLayout() {
  const { user, logout, can } = useAuth();
  const nav = useNavigate();
  const [open, setOpen] = useState(false);
  const [collapsed, setCollapsed] = useState({});
  const groups = useMemo(() => buildNav(can), [can]);

  const sidebar = (
    <nav className="flex-1 overflow-y-auto px-3 pb-6" aria-label="Application">
      {groups.map((g) => (
        <div key={g.group} className="mt-4">
          <button className="flex w-full items-center justify-between px-2 py-1 text-xs uppercase tracking-wide text-fp-muted" onClick={() => setCollapsed({ ...collapsed, [g.group]: !collapsed[g.group] })} aria-expanded={!collapsed[g.group]}>
            {g.group}<ChevronDown size={14} className={collapsed[g.group] ? '-rotate-90' : ''} aria-hidden />
          </button>
          {!collapsed[g.group] && g.items.map((i) => (
            <NavLink key={i.to} to={i.to} end={i.end} onClick={() => setOpen(false)} className={({ isActive }) => `fp-nav-link block ${isActive ? 'is-active' : ''}`}>{i.label}</NavLink>
          ))}
        </div>
      ))}
    </nav>
  );

  return (
    <div className="min-h-screen bg-fp-bg text-fp-text lg:grid lg:grid-cols-[260px_1fr]">
      <aside className="hidden h-screen flex-col border-r border-fp-border bg-fp-sidebar lg:sticky lg:top-0 lg:flex">
        <div className="px-5 py-5"><FleetPulseLogo variant="horizontal" height={28} decorative /></div>
        {sidebar}
      </aside>
      {open && (
        <div className="fixed inset-0 z-50 lg:hidden" onMouseDown={(e) => e.target === e.currentTarget && setOpen(false)} style={{ background: 'rgba(0,0,0,.6)' }}>
          <aside className="flex h-full w-72 flex-col bg-fp-sidebar">
            <div className="flex items-center justify-between px-5 py-4"><FleetPulseLogo variant="horizontal" height={26} decorative /><button onClick={() => setOpen(false)} aria-label="Close menu"><X /></button></div>
            {sidebar}
          </aside>
        </div>
      )}
      <div className="min-w-0">
        <header className="sticky top-0 z-30 flex h-16 items-center justify-between border-b border-fp-border bg-fp-bg/90 px-4 backdrop-blur sm:px-6">
          <button className="fp-btn fp-btn--ghost lg:hidden" onClick={() => setOpen(true)} aria-label="Open menu"><Menu /></button>
          <p className="hidden text-sm text-fp-muted lg:block">{user.companyName}</p>
          <div className="flex items-center gap-3">
            <div className="text-right text-sm"><p className="leading-tight">{user.fullName}</p><p className="text-xs text-fp-muted">{user.email}</p></div>
            <Badge>{user.role.replaceAll('_', ' ')}</Badge>
            <button className="fp-btn fp-btn--ghost" onClick={async () => { await logout(); nav('/login'); }} aria-label="Log out"><LogOut size={18} /></button>
          </div>
        </header>
        <main className="mx-auto max-w-[1400px] px-4 py-8 sm:px-6"><Outlet /></main>
      </div>
    </div>
  );
}
