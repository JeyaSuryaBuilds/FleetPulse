import { useEffect, useRef, useState } from 'react';
import { Send } from 'lucide-react';
import { api, errorMessage } from '../../api.js';
import { Badge, Button, ErrorNote, Field, PageHeader, Select, Textarea } from '../../ui.jsx';

const FEATURES = [['ASSISTANT', 'Fleet assistant'], ['FUEL_ANALYSIS', 'Fuel cost analysis'], ['DELAY_PREDICTION', 'Delivery delay risk'], ['ROUTE_RECOMMENDATION', 'Route recommendations'],
  ['MAINTENANCE_INSIGHTS', 'Maintenance insights'], ['DRIVER_PERFORMANCE', 'Driver performance'], ['EXPENSE_ANALYSIS', 'Expense analysis'], ['REPORT', 'Natural language report']];

export default function Ai() {
  const [status, setStatus] = useState(null);
  const [feature, setFeature] = useState('ASSISTANT');
  const [q, setQ] = useState('');
  const [msgs, setMsgs] = useState([]);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const end = useRef(null);
  useEffect(() => { api.get('/ai/status').then((r) => setStatus(r.data)).catch((e) => setError(errorMessage(e))); }, []);
  useEffect(() => { end.current?.scrollIntoView({ behavior: 'smooth' }); }, [msgs]);

  async function send(e) {
    e.preventDefault();
    if (!q.trim()) return;
    const question = q.trim();
    setQ(''); setError(''); setBusy(true);
    const history = msgs.map((m) => ({ role: m.role, content: m.content }));
    setMsgs((m) => [...m, { role: 'user', content: question }]);
    try {
      const { data } = await api.post('/ai/analyze', { feature, question, history });
      setMsgs((m) => [...m, { role: 'assistant', content: data.answer, meta: `${data.model} · data used: ${data.dataSections.join(', ') || 'none available to your role'}` }]);
      api.get('/ai/status').then((r) => setStatus(r.data)).catch(() => {});
    } catch (err) { setError(errorMessage(err)); } finally { setBusy(false); }
  }

  return (
    <>
      <PageHeader title="AI intelligence" subtitle="Answers use only your company data that your role may access"
        actions={status && <Badge tone={status.available ? 'success' : 'warning'}>{status.available ? `${status.usedThisMonth} / ${status.quota} requests this month` : 'Not available'}</Badge>} />
      {status && !status.available && <div className="fp-card fp-card--padded mb-4 text-sm text-fp-muted">AI is not active yet. The platform administrator must configure the NVIDIA NIM API key, choose a model and enable AI.</div>}
      <div className="fp-card fp-card--padded">
        <Field label="Analysis type" className="max-w-xs"><Select value={feature} onChange={(e) => setFeature(e.target.value)}>{FEATURES.map(([v, l]) => <option key={v} value={v}>{l}</option>)}</Select></Field>
        <div className="mt-4 max-h-[420px] min-h-[160px] space-y-3 overflow-y-auto rounded-fp-md border border-fp-border bg-fp-inset p-3">
          {msgs.length === 0 && <p className="text-sm text-fp-muted">Ask a question, for example “Which vehicles had the highest fuel cost in the last 90 days?”</p>}
          {msgs.map((m, i) => <div key={i} className={m.role === 'user' ? 'text-right' : ''}><div className={`inline-block max-w-[85%] whitespace-pre-wrap rounded-fp-md px-3 py-2 text-left text-sm ${m.role === 'user' ? 'bg-orange text-fp-on-orange' : 'border border-fp-border bg-fp-card'}`}>{m.content}</div>{m.meta && <p className="mt-1 text-xs text-fp-muted">{m.meta}</p>}</div>)}
          <div ref={end} />
        </div>
        <ErrorNote>{error}</ErrorNote>
        <form onSubmit={send} className="mt-3 flex gap-2"><Textarea className="min-h-[44px] flex-1" rows={2} maxLength={2000} value={q} onChange={(e) => setQ(e.target.value)} placeholder="Ask about your fleet" /><Button type="submit" loading={busy} disabled={!status?.available}><Send size={16} aria-hidden /> Send</Button></form>
        <p className="mt-2 text-xs text-fp-muted">AI answers can be wrong. Check important figures against your reports. Delay predictions are statistical judgements from past data, not a trained forecasting model.</p>
      </div>
    </>
  );
}
