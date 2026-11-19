import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api, adminSession, errorMessage } from '../../api.js';
import { Button, EmptyState, ErrorNote, Field, Input, PageHeader, Spinner, StatusBadge, fmtDateTime, useToast } from '../../ui.jsx';
import { useGet } from './adminUtil.js';

export default function AdminSecurity() {
  const toast = useToast();
  const nav = useNavigate();
  const [tab, setTab] = useState('admin');
  const login = useGet('/admin/security/login-activity', { subjectType: tab === 'users' ? 'USER' : 'ADMIN' });
  const audit = useGet('/admin/security/audit-logs');
  const [pw, setPw] = useState({ currentPassword: '', newPassword: '', confirm: '' });
  const [err, setErr] = useState('');

  async function change(e) {
    e.preventDefault(); setErr('');
    if (pw.newPassword !== pw.confirm) { setErr('New passwords do not match'); return; }
    try { await api.post('/admin/security/change-password', { currentPassword: pw.currentPassword, newPassword: pw.newPassword }); toast('Password changed. Please sign in again.'); adminSession.clear(); nav('/admin'); }
    catch (x) { setErr(errorMessage(x)); }
  }
  async function logoutAll() {
    if (!window.confirm('Sign out every Super Admin session, including this one?')) return;
    try { await api.post('/admin/auth/logout-all'); adminSession.clear(); nav('/admin'); } catch (x) { setErr(errorMessage(x)); }
  }
  return (
    <>
      <PageHeader title="Security" subtitle="Admin password, sessions, login activity and audit logs" />
      <ErrorNote>{err}</ErrorNote>
      <div className="grid gap-6 lg:grid-cols-2">
        <form onSubmit={change} className="fp-card fp-card--padded space-y-3"><h2 className="font-medium">Change Super Admin password</h2>
          <p className="text-sm text-fp-muted">The initial password is a shared bootstrap secret. Replace it with a long, unique password now (12 characters or more).</p>
          <Field label="Current password"><Input type="password" required autoComplete="current-password" value={pw.currentPassword} onChange={(e) => setPw({ ...pw, currentPassword: e.target.value })} /></Field>
          <Field label="New password"><Input type="password" required minLength={12} autoComplete="new-password" value={pw.newPassword} onChange={(e) => setPw({ ...pw, newPassword: e.target.value })} /></Field>
          <Field label="Confirm new password"><Input type="password" required autoComplete="new-password" value={pw.confirm} onChange={(e) => setPw({ ...pw, confirm: e.target.value })} /></Field><Button type="submit">Change password</Button></form>
        <div className="fp-card fp-card--padded"><h2 className="font-medium">Session management</h2><p className="mt-2 text-sm text-fp-muted">Admin sessions expire automatically. Use this if you suspect the password was exposed.</p><Button variant="danger" className="mt-3" onClick={logoutAll}>Sign out all admin sessions</Button></div>
      </div>
      <h2 className="mb-2 mt-8 font-medium">Login activity</h2>
      <div className="mb-3 inline-flex rounded-fp-md border border-fp-border p-1">{[['admin', 'Super Admin'], ['users', 'Company users']].map(([id, l]) => <button key={id} onClick={() => setTab(id)} aria-pressed={tab === id} className={`rounded-fp-sm px-4 py-1.5 text-sm ${tab === id ? 'bg-orange text-fp-on-orange font-medium' : 'text-fp-muted'}`}>{l}</button>)}</div>
      <div className="fp-card max-h-96 overflow-auto">{!login.data ? <Spinner /> : login.data.length === 0 ? <EmptyState title="No activity" /> : (
        <table className="fp-table"><thead><tr><th>Time</th><th>Identifier</th><th>Result</th><th>Reason</th><th>IP</th></tr></thead><tbody>{login.data.map((l) => <tr key={l.id}><td>{fmtDateTime(l.createdAt)}</td><td>{l.identifier || '—'}</td><td><StatusBadge value={l.success ? 'SUCCEEDED' : 'FAILED'} /></td><td>{l.reason || '—'}</td><td>{l.ipAddress}</td></tr>)}</tbody></table>)}</div>
      <h2 className="mb-2 mt-8 font-medium">Audit logs</h2>
      <div className="fp-card max-h-96 overflow-auto">{!audit.data ? <Spinner /> : audit.data.length === 0 ? <EmptyState title="No audit entries" /> : (
        <table className="fp-table"><thead><tr><th>Time</th><th>Company</th><th>Actor</th><th>Action</th><th>Entity</th><th>IP</th></tr></thead><tbody>{audit.data.map((l) => <tr key={l.id}><td>{fmtDateTime(l.createdAt)}</td><td>{l.companyId ?? '—'}</td><td>{l.actorType} {l.actorId ?? ''}</td><td>{l.action}</td><td>{l.entityType ? `${l.entityType} ${l.entityId ?? ''}` : '—'}</td><td>{l.ipAddress}</td></tr>)}</tbody></table>)}</div>
    </>
  );
}
