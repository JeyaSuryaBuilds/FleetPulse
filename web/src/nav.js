import { LayoutDashboard, MapPinned, Bell, Sparkles, FileBarChart, LineChart, Route, Wallet, Building2, Users, CreditCard, UserRound } from 'lucide-react';
import { resources, groupsOrder } from './resources.js';

export const customPages = [
  { group: 'Overview', to: '', label: 'Dashboard', icon: LayoutDashboard, perm: null },
  { group: 'Overview', to: 'me', label: 'My work and pay', icon: UserRound, perm: 'SELF_SERVICE' },
  { group: 'Overview', to: 'live-map', label: 'Live map', icon: MapPinned, perm: 'GPS_READ' },
  { group: 'Overview', to: 'notifications', label: 'Notifications', icon: Bell, perm: 'NOTIFICATION_READ' },
  { group: 'Overview', to: 'ai', label: 'AI assistant', icon: Sparkles, perm: 'AI_USE' },
  { group: 'Overview', to: 'reports', label: 'Reports', icon: FileBarChart, perm: 'REPORT_VIEW' },
  { group: 'Fleet', to: 'analytics', label: 'Fleet analytics', icon: LineChart, perm: 'FLEET_READ' },
  { group: 'Operations', to: 'route-planner', label: 'Route planner', icon: Route, perm: 'ROUTE_READ' },
  { group: 'Payroll', to: 'payroll-runs', label: 'Payroll runs', icon: Wallet, perm: 'PAYROLL_READ' },
  { group: 'Administration', to: 'company', label: 'Company profile', icon: Building2, perm: 'DASHBOARD_VIEW' },
  { group: 'Administration', to: 'users', label: 'Users and roles', icon: Users, perm: 'USER_MANAGE' },
  { group: 'Administration', to: 'subscription', label: 'Subscription', icon: CreditCard, perm: 'SUBSCRIPTION_MANAGE' },
];

export function buildNav(can) {
  const order = ['Overview', ...groupsOrder, 'Administration'];
  const groups = order.map((g) => ({
    group: g,
    items: [
      ...customPages.filter((p) => p.group === g && (p.perm === null || can(p.perm))).map((p) => ({ to: `/app/${p.to}`.replace(/\/$/, ''), label: p.label, end: p.to === '' })),
      ...resources.filter((r) => r.group === g && can(r.read)).map((r) => ({ to: `/app/${r.path}`, label: r.title })),
    ],
  }));
  return groups.filter((g) => g.items.length);
}
