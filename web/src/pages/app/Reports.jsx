import { useEffect, useState } from 'react';
import { Download } from 'lucide-react';
import { api, errorMessage } from '../../api.js';
import { Button, EmptyState, ErrorNote, Field, Input, PageHeader, Spinner, useToast } from '../../ui.jsx';

const iso = (d) => d.toISOString().slice(0, 10);

export default function Reports() {
  const toast = useToast();
  const [list, setList] = useState(null);
  const [type, setType] = useState('');
  const [from, setFrom] = useState(iso(new Date(Date.now() - 30 * 864e5)));
  const [to, setTo] = useState(iso(new Date()));
  const [rows, setRows] = useState(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => { api.get('/reports').then((r) => { setList(r.data); if (r.data[0]) setType(r.data[0].type); }).catch((e) => setError(errorMessage(e))); }, []);
  useEffect(() => {
    if (!type) return;
    setRows(null); setError('');
    api.get(`/reports/${type}`, { params: { from, to } }).then((r) => setRows(r.data)).catch((e) => { setRows([]); setError(errorMessage(e)); });
  }, [type, from, to]);

  async function download(format) {
    setBusy(true);
    try {
      const res = await api.get(`/reports/${type}/export`, { params: { format, from, to }, responseType: 'blob' });
      const url = URL.createObjectURL(res.data); const a = document.createElement('a'); a.href = url; a.download = `fleetpulse-${type}-${from}_${to}.${format}`; a.click(); URL.revokeObjectURL(url);
    } catch (e) { toast('Export failed', 'error'); } finally { setBusy(false); }
  }

  if (!list) return <Spinner />;
  const keys = rows?.[0] ? Object.keys(rows[0]) : [];
  return (
    <>
      <PageHeader title="Reports" subtitle="Export as PDF or CSV" actions={<><Button variant="secondary" loading={busy} onClick={() => download('csv')}><Download size={16} aria-hidden /> CSV</Button><Button loading={busy} onClick={() => download('pdf')}><Download size={16} aria-hidden /> PDF</Button></>} />
      <div className="fp-card fp-card--padded mb-4 grid gap-4 sm:grid-cols-3">
        <Field label="Report"><select className="fp-input" value={type} onChange={(e) => setType(e.target.value)}>{list.map((r) => <option key={r.type} value={r.type}>{r.title}</option>)}</select></Field>
        <Field label="From"><Input type="date" value={from} onChange={(e) => setFrom(e.target.value)} /></Field>
        <Field label="To"><Input type="date" value={to} onChange={(e) => setTo(e.target.value)} /></Field>
      </div>
      <ErrorNote>{error}</ErrorNote>
      <div className="fp-card overflow-x-auto">{!rows ? <Spinner /> : rows.length === 0 ? <EmptyState title="No records for this period" /> : (
        <table className="fp-table"><thead><tr>{keys.map((k) => <th key={k}>{k.replaceAll('_', ' ')}</th>)}</tr></thead><tbody>{rows.slice(0, 200).map((r, i) => <tr key={i}>{keys.map((k) => <td key={k}>{r[k] === null ? '—' : String(r[k])}</td>)}</tr>)}</tbody></table>)}</div>
      {rows && rows.length > 200 && <p className="mt-2 text-xs text-fp-muted">Showing the first 200 rows. Export for the full report.</p>}
    </>
  );
}
