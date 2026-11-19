import { useEffect, useState } from 'react';
import { api, errorMessage } from '../../api.js';
import { Badge, Button, EmptyState, ErrorNote, Field, Input, PageHeader, Spinner, fmtMoney, useToast } from '../../ui.jsx';
import { useGet } from './adminUtil.js';

export default function AdminAi() {
  const toast = useToast();
  const cfg = useGet('/admin/ai/config');
  const models = useGet('/admin/ai/models');
  const usage = useGet('/admin/ai/usage');
  const [c, setC] = useState(null);
  const [m, setM] = useState({ modelId: '', displayName: '', active: true, inputCostPer1k: 0, outputCostPer1k: 0 });
  const [err, setErr] = useState('');
  useEffect(() => { if (cfg.data) setC(cfg.data); }, [cfg.data]);

  async function saveCfg() {
    try { await api.put('/admin/ai/config', { enabled: c.enabled, defaultModel: c.defaultModel, rateLimitPerMinute: Number(c.rateLimitPerMinute) }); toast('AI configuration saved'); cfg.reload(); } catch (e) { setErr(errorMessage(e)); }
  }
  async function addModel(e) {
    e.preventDefault();
    try { await api.post('/admin/ai/models', { ...m, inputCostPer1k: Number(m.inputCostPer1k), outputCostPer1k: Number(m.outputCostPer1k) }); toast('Model added'); setM({ ...m, modelId: '', displayName: '' }); models.reload(); } catch (x) { setErr(errorMessage(x)); }
  }
  async function toggle(x) {
    try { await api.put(`/admin/ai/models/${x.id}`, { modelId: x.modelId, displayName: x.displayName, active: !x.active, inputCostPer1k: x.inputCostPer1k, outputCostPer1k: x.outputCostPer1k }); models.reload(); } catch (e) { setErr(errorMessage(e)); }
  }
  if (!c) return <Spinner />;
  return (
    <>
      <PageHeader title="AI management" subtitle="NVIDIA NIM configuration, models, usage and cost" />
      <ErrorNote>{err || cfg.error}</ErrorNote>
      <div className="fp-card fp-card--padded mb-6 grid max-w-3xl gap-4 sm:grid-cols-2">
        <div className="sm:col-span-2 flex items-center gap-2 text-sm">API key: {c.apiKeyConfigured ? <Badge tone="success">configured on the server</Badge> : <Badge tone="warning">not set. Set FP_NVIDIA_API_KEY in the backend environment</Badge>}</div>
        <label className="flex items-center gap-2"><input type="checkbox" checked={c.enabled} onChange={(e) => setC({ ...c, enabled: e.target.checked })} /> Enable AI features for all companies</label>
        <Field label="Rate limit (requests / user / minute)"><Input type="number" min="1" value={c.rateLimitPerMinute} onChange={(e) => setC({ ...c, rateLimitPerMinute: e.target.value })} /></Field>
        <Field label="Default model ID" className="sm:col-span-2" hint="Exactly as listed in your NVIDIA NIM catalogue, e.g. meta/llama-3.1-70b-instruct"><Input value={c.defaultModel} onChange={(e) => setC({ ...c, defaultModel: e.target.value })} list="models" /></Field>
        <datalist id="models">{models.data?.map((x) => <option key={x.id} value={x.modelId} />)}</datalist>
        <div className="sm:col-span-2"><Button onClick={saveCfg}>Save configuration</Button></div>
      </div>

      <h2 className="mb-2 font-medium">Model registry (for cost analytics)</h2>
      <div className="fp-card mb-3 overflow-x-auto">{!models.data ? <Spinner /> : models.data.length === 0 ? <EmptyState title="No models registered" /> : (
        <table className="fp-table"><thead><tr><th>Model</th><th>Name</th><th>$ / 1k input</th><th>$ / 1k output</th><th>Active</th></tr></thead><tbody>{models.data.map((x) => <tr key={x.id}><td>{x.modelId}</td><td>{x.displayName}</td><td>{x.inputCostPer1k}</td><td>{x.outputCostPer1k}</td><td><Button variant="ghost" onClick={() => toggle(x)}>{x.active ? 'Active' : 'Inactive'}</Button></td></tr>)}</tbody></table>)}</div>
      <form onSubmit={addModel} className="fp-card fp-card--padded mb-8 grid items-end gap-3 sm:grid-cols-5">
        <Field label="Model ID"><Input required value={m.modelId} onChange={(e) => setM({ ...m, modelId: e.target.value })} /></Field><Field label="Display name"><Input required value={m.displayName} onChange={(e) => setM({ ...m, displayName: e.target.value })} /></Field>
        <Field label="Input cost / 1k"><Input type="number" step="any" min="0" value={m.inputCostPer1k} onChange={(e) => setM({ ...m, inputCostPer1k: e.target.value })} /></Field><Field label="Output cost / 1k"><Input type="number" step="any" min="0" value={m.outputCostPer1k} onChange={(e) => setM({ ...m, outputCostPer1k: e.target.value })} /></Field><Button type="submit">Add model</Button>
      </form>

      <h2 className="mb-2 font-medium">Usage this month by company</h2>
      <div className="fp-card overflow-x-auto">{!usage.data ? <Spinner /> : usage.data.length === 0 ? <EmptyState title="No AI usage yet" /> : (
        <table className="fp-table"><thead><tr><th>Company</th><th>Requests</th><th>Prompt tokens</th><th>Completion tokens</th><th>Estimated cost</th></tr></thead><tbody>{usage.data.map((u) => <tr key={u.companyId}><td>{u.company}</td><td>{u.requests}</td><td>{Number(u.promptTokens).toLocaleString()}</td><td>{Number(u.completionTokens).toLocaleString()}</td><td>{fmtMoney(u.estimatedCost)}</td></tr>)}</tbody></table>)}</div>
    </>
  );
}
