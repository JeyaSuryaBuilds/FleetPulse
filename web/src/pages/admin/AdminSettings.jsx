import { useEffect, useState } from 'react';
import { api, errorMessage } from '../../api.js';
import { Button, ErrorNote, Field, Input, PageHeader, Spinner, useToast } from '../../ui.jsx';
import { useGet } from './adminUtil.js';

const EDITABLE = /^(general|email|sms|payment|branding)\./;

export default function AdminSettings() {
  const toast = useToast();
  const { data, error, reload } = useGet('/admin/settings');
  const [v, setV] = useState(null);
  const [err, setErr] = useState('');
  useEffect(() => { if (data) setV(Object.fromEntries(Object.entries(data).filter(([k]) => EDITABLE.test(k)))); }, [data]);
  async function save(e) { e.preventDefault(); try { await api.put('/admin/settings', v); toast('Settings saved'); reload(); } catch (x) { setErr(errorMessage(x)); } }
  if (!v) return error ? <ErrorNote>{error}</ErrorNote> : <Spinner />;
  return (
    <>
      <PageHeader title="Configuration" subtitle="General, email, SMS, payment and branding settings" />
      <ErrorNote>{err}</ErrorNote>
      <form onSubmit={save} className="fp-card fp-card--padded grid max-w-3xl gap-4 sm:grid-cols-2">
        {Object.keys(v).map((k) => <Field key={k} label={k.replace('.', ' · ').replaceAll('_', ' ')}><Input value={v[k] ?? ''} onChange={(e) => setV({ ...v, [k]: e.target.value })} /></Field>)}
        <div className="sm:col-span-2"><Button type="submit">Save settings</Button></div>
      </form>
      <div className="fp-card fp-card--padded mt-6 max-w-3xl text-sm text-fp-muted"><p className="font-medium text-fp-text">Secrets are not stored here</p><p className="mt-1">SMTP, Twilio, Stripe, NVIDIA, routing, storage and WhatsApp credentials are read from the backend server environment (see .env.example). Platform branding assets (logo, icons) are part of the application build.</p></div>
    </>
  );
}
