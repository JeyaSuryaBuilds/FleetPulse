import { useEffect, useState } from 'react';
import { api, errorMessage } from '../../api.js';
import { Badge, Button, ErrorNote, Field, Input, PageHeader, Spinner, Stat, useToast } from '../../ui.jsx';
import { useGet } from './adminUtil.js';

export default function AdminPlatform() {
  const toast = useToast();
  const health = useGet('/admin/platform/health');
  const maint = useGet('/admin/platform/maintenance');
  const flags = useGet('/admin/platform/flags');
  const [m, setM] = useState(null);
  const [nf, setNf] = useState({ flagKey: '', description: '' });
  const [err, setErr] = useState('');
  useEffect(() => { if (maint.data) setM(maint.data); }, [maint.data]);

  async function saveMaint() {
    if (m.enabled && !window.confirm('Turn ON maintenance mode? All company and public APIs will return 503 until you turn it off.')) return;
    try { await api.put('/admin/platform/maintenance', m); toast('Maintenance settings saved'); } catch (e) { setErr(errorMessage(e)); }
  }
  async function flag(f, enabled) { try { await api.put('/admin/platform/flags', { flagKey: f.flagKey, description: f.description, enabled }); flags.reload(); } catch (e) { setErr(errorMessage(e)); } }
  async function addFlag(e) { e.preventDefault(); try { await api.put('/admin/platform/flags', { ...nf, enabled: false }); setNf({ flagKey: '', description: '' }); flags.reload(); } catch (x) { setErr(errorMessage(x)); } }

  const h = health.data;
  return (
    <>
      <PageHeader title="Platform management" subtitle="System monitoring, feature flags and maintenance mode" />
      <ErrorNote>{err || health.error}</ErrorNote>
      {!h ? <Spinner /> : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <Stat label="Database" value={h.databaseOk ? 'OK' : 'Down'} tone={h.databaseOk ? 'success' : 'error'} sub={`${h.databaseLatencyMs} ms`} /><Stat label="Memory" value={`${h.jvmUsedMb} / ${h.jvmMaxMb} MB`} sub={`${h.processors} CPUs`} />
          <Stat label="Database size" value={`${h.database.sizeMb} MB`} /><Stat label="GPS rows" value={Number(h.database.gpsRows).toLocaleString()} sub={`${Number(h.database.auditRows).toLocaleString()} audit rows`} />
        </div>)}
      <div className="fp-card fp-card--padded mt-6 max-w-2xl space-y-3"><h2 className="font-medium">Maintenance mode</h2>
        {!m ? <Spinner /> : (<><label className="flex items-center gap-2"><input type="checkbox" checked={m.enabled} onChange={(e) => setM({ ...m, enabled: e.target.checked })} /> Maintenance mode is {m.enabled ? <Badge tone="error">ON</Badge> : <Badge tone="success">off</Badge>}</label>
          <Field label="Message shown to users"><Input value={m.message} onChange={(e) => setM({ ...m, message: e.target.value })} maxLength={500} /></Field><Button onClick={saveMaint}>Save</Button></>)}</div>
      <h2 className="mb-2 mt-8 font-medium">Feature flags</h2>
      <div className="fp-card divide-y divide-fp-border">{!flags.data ? <Spinner /> : flags.data.map((f) => (
        <div key={f.id} className="flex items-center justify-between gap-3 px-4 py-3"><div><p className="font-mono text-sm">{f.flagKey}</p><p className="text-xs text-fp-muted">{f.description}</p></div><label className="flex items-center gap-2 text-sm"><input type="checkbox" checked={!!f.enabled} onChange={(e) => flag(f, e.target.checked)} /> {f.enabled ? 'On' : 'Off'}</label></div>))}</div>
      <form onSubmit={addFlag} className="mt-3 flex max-w-2xl flex-wrap items-end gap-3"><Field label="New flag key"><Input required pattern="[a-z0-9_]{2,80}" value={nf.flagKey} onChange={(e) => setNf({ ...nf, flagKey: e.target.value })} /></Field><Field label="Description" className="flex-1"><Input value={nf.description} onChange={(e) => setNf({ ...nf, description: e.target.value })} /></Field><Button type="submit">Add</Button></form>
      <p className="mt-4 text-xs text-fp-muted">Storage and external API health checks need provider credentials and are not included yet.</p>
    </>
  );
}
