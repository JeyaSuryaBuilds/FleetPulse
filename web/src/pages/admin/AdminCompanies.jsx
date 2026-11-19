import { useState } from 'react';
import { api, errorMessage } from '../../api.js';
import { Button, EmptyState, ErrorNote, Field, Input, Modal, PageHeader, Select, Spinner, StatusBadge, fmtDate, fmtDateTime, useToast } from '../../ui.jsx';
import { useGet } from './adminUtil.js';

export default function AdminCompanies() {
  const toast = useToast();
  const [status, setStatus] = useState('');
  const [q, setQ] = useState('');
  const [page, setPage] = useState(0);
  const list = useGet('/admin/companies', { status: status || undefined, q: q || undefined, page, size: 25 });
  const plans = useGet('/admin/plans');
  const [sel, setSel] = useState(null);
  const [detail, setDetail] = useState(null);
  const [f, setF] = useState({});
  const [err, setErr] = useState('');

  async function open(id) {
    setSel(id); setDetail(null); setErr('');
    const { data } = await api.get(`/admin/companies/${id}`);
    setDetail(data); setF({ planCode: data.subscription?.[0]?.planCode || '', billingCycle: data.subscription?.[0]?.billingCycle || 'MONTHLY', extendTrialDays: '' });
  }
  async function setCompanyStatus(s) {
    try { await api.patch(`/admin/companies/${sel}/status`, { status: s }); toast(`Company set to ${s}`); open(sel); list.reload(); } catch (e) { setErr(errorMessage(e)); }
  }
  async function saveSub() {
    try { await api.put(`/admin/companies/${sel}/subscription`, { planCode: f.planCode || null, billingCycle: f.billingCycle, extendTrialDays: f.extendTrialDays ? Number(f.extendTrialDays) : null }); toast('Subscription updated'); open(sel); list.reload(); } catch (e) { setErr(errorMessage(e)); }
  }

  const rows = list.data?.content;
  return (
    <>
      <PageHeader title="Companies" subtitle="Companies are active from registration. There is no approval step." />
      <div className="mb-4 flex flex-wrap gap-2">
        <Input className="max-w-xs" placeholder="Search name, email or mobile" aria-label="Search" value={q} onChange={(e) => { setQ(e.target.value); setPage(0); }} />
        <Select className="max-w-[12rem]" aria-label="Status" value={status} onChange={(e) => { setStatus(e.target.value); setPage(0); }}><option value="">All statuses</option>{['ACTIVE', 'TRIAL', 'SUSPENDED', 'EXPIRED', 'CANCELLED'].map((s) => <option key={s}>{s}</option>)}</Select>
      </div>
      <ErrorNote>{list.error}</ErrorNote>
      <div className="fp-card overflow-x-auto">{!rows ? <Spinner /> : rows.length === 0 ? <EmptyState title="No companies found" /> : (
        <table className="fp-table"><thead><tr><th>Company</th><th>Email</th><th>Plan</th><th>Users</th><th>Vehicles</th><th>Status</th><th>Created</th><th /></tr></thead>
          <tbody>{rows.map((c) => <tr key={c.id}><td>{c.name}</td><td>{c.email}</td><td>{c.plan || '—'}</td><td>{c.users}</td><td>{c.vehicles}</td><td><StatusBadge value={c.status} /></td><td>{fmtDate(c.createdAt)}</td><td><Button variant="ghost" onClick={() => open(c.id)}>Details</Button></td></tr>)}</tbody></table>)}</div>
      {list.data && list.data.total > 25 && <div className="mt-3 flex items-center justify-end gap-2 text-sm text-fp-muted"><Button variant="ghost" disabled={page === 0} onClick={() => setPage(page - 1)}>Previous</Button>Page {page + 1}<Button variant="ghost" disabled={(page + 1) * 25 >= list.data.total} onClick={() => setPage(page + 1)}>Next</Button></div>}

      <Modal open={sel !== null} onClose={() => setSel(null)} title={detail?.name || 'Company'} wide>
        {!detail ? <Spinner /> : (
          <div className="space-y-5 text-sm">
            <div className="grid gap-3 sm:grid-cols-2"><div><p className="text-xs text-fp-muted">Email</p><p>{detail.email}</p></div><div><p className="text-xs text-fp-muted">Mobile</p><p>{detail.mobile}</p></div><div><p className="text-xs text-fp-muted">Status</p><StatusBadge value={detail.status} /></div><div><p className="text-xs text-fp-muted">Registered</p><p>{fmtDateTime(detail.createdAt)}</p></div></div>
            <div><p className="mb-2 font-medium">Usage</p><div className="flex flex-wrap gap-4 text-fp-muted">{Object.entries(detail.usage).map(([k, v]) => <span key={k}>{k.replace(/([A-Z])/g, ' $1').toLowerCase()}: <b className="text-fp-text">{String(v)}</b></span>)}</div></div>
            <div><p className="mb-2 font-medium">Status</p><div className="flex flex-wrap gap-2">{detail.status !== 'SUSPENDED' ? <Button variant="danger" onClick={() => setCompanyStatus('SUSPENDED')}>Suspend</Button> : <Button onClick={() => setCompanyStatus('ACTIVE')}>Reactivate</Button>}</div></div>
            <div><p className="mb-2 font-medium">Subscription</p>
              <div className="grid items-end gap-3 sm:grid-cols-4"><Field label="Plan"><Select value={f.planCode} onChange={(e) => setF({ ...f, planCode: e.target.value })}>{plans.data?.map((p) => <option key={p.code} value={p.code}>{p.name}</option>)}</Select></Field>
                <Field label="Billing"><Select value={f.billingCycle} onChange={(e) => setF({ ...f, billingCycle: e.target.value })}><option>MONTHLY</option><option>ANNUAL</option></Select></Field>
                <Field label="Extend trial (days)"><Input type="number" min="0" max="365" value={f.extendTrialDays} onChange={(e) => setF({ ...f, extendTrialDays: e.target.value })} /></Field><Button onClick={saveSub}>Apply</Button></div>
              {detail.subscription?.[0] && <p className="mt-2 text-fp-muted">Current: {detail.subscription[0].plan} · {detail.subscription[0].status} · period ends {fmtDate(detail.subscription[0].currentPeriodEnd)}</p>}</div>
            <div><p className="mb-2 font-medium">Recent activity</p>{detail.recentActivity.length === 0 ? <p className="text-fp-muted">None.</p> : <ul className="space-y-1 text-fp-muted">{detail.recentActivity.map((a, i) => <li key={i}>{fmtDateTime(a.createdAt)} · {a.action.replaceAll('_', ' ').toLowerCase()}</li>)}</ul>}</div>
            <ErrorNote>{err}</ErrorNote>
          </div>)}
      </Modal>
    </>
  );
}
