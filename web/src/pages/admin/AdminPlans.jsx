import { useState } from 'react';
import { Plus } from 'lucide-react';
import { api, errorMessage } from '../../api.js';
import { Button, ErrorNote, Field, Input, Modal, PageHeader, Spinner, StatusBadge, fmtMoney, useToast } from '../../ui.jsx';
import { useGet } from './adminUtil.js';

const blank = { code: '', name: '', description: '', monthlyPrice: 0, annualPrice: 0, currency: 'USD', trialDays: 14, vehicleLimit: 10, userLimit: 5, aiMonthlyQuota: 0, gpsMonthlyQuota: 0, featuresJson: '', active: true, sortOrder: 0 };

export default function AdminPlans() {
  const toast = useToast();
  const { data, error, reload } = useGet('/admin/plans');
  const [f, setF] = useState(null);
  const [isNew, setIsNew] = useState(false);
  const [err, setErr] = useState('');
  const set = (k, num) => (e) => setF({ ...f, [k]: num ? Number(e.target.value) : e.target.value });

  function openEdit(p) {
    setErr(''); setIsNew(!p);
    setF(p ? { code: p.code, name: p.name, description: p.description || '', monthlyPrice: p.monthly_price, annualPrice: p.annual_price, currency: p.currency, trialDays: p.trial_days, vehicleLimit: p.vehicle_limit, userLimit: p.user_limit, aiMonthlyQuota: p.ai_monthly_quota, gpsMonthlyQuota: p.gps_monthly_quota, featuresJson: typeof p.features_json === 'string' ? p.features_json : '', active: !!p.active, sortOrder: p.sort_order } : blank);
  }
  async function save(e) {
    e.preventDefault();
    try {
      const body = { ...f, featuresJson: f.featuresJson || null };
      if (isNew) await api.post('/admin/plans', body); else await api.put(`/admin/plans/${f.code}`, body);
      toast('Plan saved'); setF(null); reload();
    } catch (x) { setErr(errorMessage(x)); }
  }
  return (
    <>
      <PageHeader title="Plans and pricing" subtitle="The public pricing page and registration read these values." actions={<Button onClick={() => openEdit(null)}><Plus size={16} aria-hidden /> New plan</Button>} />
      <ErrorNote>{error}</ErrorNote>
      <div className="fp-card overflow-x-auto">{!data ? <Spinner /> : (
        <table className="fp-table"><thead><tr><th>Plan</th><th>Monthly</th><th>Annual</th><th>Trial</th><th>Vehicles</th><th>Users</th><th>AI / mo</th><th>GPS / mo</th><th>Status</th><th /></tr></thead>
          <tbody>{data.map((p) => <tr key={p.code}><td>{p.name}<br /><span className="text-xs text-fp-muted">{p.code}</span></td><td>{fmtMoney(p.monthly_price, p.currency)}</td><td>{fmtMoney(p.annual_price, p.currency)}</td><td>{p.trial_days}d</td><td>{p.vehicle_limit}</td><td>{p.user_limit}</td><td>{p.ai_monthly_quota}</td><td>{Number(p.gps_monthly_quota).toLocaleString()}</td><td><StatusBadge value={p.active ? 'ACTIVE' : 'DISABLED'} /></td><td><Button variant="ghost" onClick={() => openEdit(p)}>Edit</Button></td></tr>)}</tbody></table>)}</div>
      <Modal open={!!f} onClose={() => setF(null)} title={isNew ? 'New plan' : `Edit ${f?.name}`} wide footer={<><Button variant="ghost" onClick={() => setF(null)}>Cancel</Button><Button type="submit" form="pf">Save</Button></>}>
        {f && <form id="pf" onSubmit={save} className="grid gap-4 sm:grid-cols-2">
          <Field label="Code (A-Z, 0-9, _)"><Input required disabled={!isNew} pattern="[A-Z0-9_]{2,40}" value={f.code} onChange={set('code')} /></Field><Field label="Name"><Input required value={f.name} onChange={set('name')} /></Field>
          <Field label="Description" className="sm:col-span-2"><Input value={f.description} onChange={set('description')} /></Field>
          <Field label="Monthly price"><Input type="number" step="0.01" min="0" value={f.monthlyPrice} onChange={set('monthlyPrice', true)} /></Field><Field label="Annual price"><Input type="number" step="0.01" min="0" value={f.annualPrice} onChange={set('annualPrice', true)} /></Field>
          <Field label="Currency"><Input maxLength={3} minLength={3} value={f.currency} onChange={set('currency')} /></Field><Field label="Trial days"><Input type="number" min="0" max="365" value={f.trialDays} onChange={set('trialDays', true)} /></Field>
          <Field label="Vehicle limit"><Input type="number" min="1" value={f.vehicleLimit} onChange={set('vehicleLimit', true)} /></Field><Field label="User limit"><Input type="number" min="1" value={f.userLimit} onChange={set('userLimit', true)} /></Field>
          <Field label="AI requests / month"><Input type="number" min="0" value={f.aiMonthlyQuota} onChange={set('aiMonthlyQuota', true)} /></Field><Field label="GPS points / month"><Input type="number" min="0" value={f.gpsMonthlyQuota} onChange={set('gpsMonthlyQuota', true)} /></Field>
          <Field label="Sort order"><Input type="number" value={f.sortOrder} onChange={set('sortOrder', true)} /></Field><label className="flex items-center gap-2 pt-7"><input type="checkbox" checked={f.active} onChange={(e) => setF({ ...f, active: e.target.checked })} /> Active (visible and purchasable)</label>
          <Field label="Feature access (JSON, optional)" className="sm:col-span-2"><Input value={f.featuresJson} onChange={set('featuresJson')} placeholder='{"ai":true}' /></Field>
          <div className="sm:col-span-2"><ErrorNote>{err}</ErrorNote></div></form>}
      </Modal>
    </>
  );
}
