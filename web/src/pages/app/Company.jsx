import { useEffect, useState } from 'react';
import { api, errorMessage } from '../../api.js';
import { useAuth } from '../../auth.jsx';
import { Button, ErrorNote, Field, Input, PageHeader, Spinner, StatusBadge, useToast } from '../../ui.jsx';

export default function Company() {
  const { can } = useAuth();
  const toast = useToast();
  const [c, setC] = useState(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  useEffect(() => { api.get('/company').then((r) => setC(r.data)).catch((e) => setError(errorMessage(e))); }, []);
  const set = (k) => (e) => setC({ ...c, [k]: e.target.value });
  async function save(e) {
    e.preventDefault(); setBusy(true); setError('');
    try { const { data } = await api.put('/company', { name: c.name, address: c.address || null, country: c.country || null, timezone: c.timezone, currency: c.currency, logoUrl: c.logoUrl || null }); setC(data); toast('Company profile saved'); }
    catch (err) { setError(errorMessage(err)); } finally { setBusy(false); }
  }
  if (!c) return error ? <ErrorNote>{error}</ErrorNote> : <Spinner />;
  const ro = !can('ORG_MANAGE');
  return (
    <>
      <PageHeader title="Company profile" subtitle="Branches, departments and warehouses are under Organization in the menu." actions={<StatusBadge value={c.status} />} />
      <form onSubmit={save} className="fp-card fp-card--padded grid max-w-3xl gap-4 sm:grid-cols-2">
        <Field label="Company name"><Input required disabled={ro} value={c.name || ''} onChange={set('name')} /></Field>
        <Field label="Email (verified)"><Input disabled value={c.email || ''} /></Field>
        <Field label="Mobile (verified)"><Input disabled value={c.mobile || ''} /></Field>
        <Field label="Country"><Input disabled={ro} value={c.country || ''} onChange={set('country')} /></Field>
        <Field label="Time zone"><Input disabled={ro} value={c.timezone || ''} onChange={set('timezone')} /></Field>
        <Field label="Currency (3 letters)"><Input disabled={ro} minLength={3} maxLength={3} value={c.currency || ''} onChange={set('currency')} /></Field>
        <Field label="Address" className="sm:col-span-2"><Input disabled={ro} value={c.address || ''} onChange={set('address')} /></Field>
        <div className="sm:col-span-2"><ErrorNote>{error}</ErrorNote></div>
        {!ro && <div className="sm:col-span-2"><Button type="submit" loading={busy}>Save</Button></div>}
      </form>
    </>
  );
}
