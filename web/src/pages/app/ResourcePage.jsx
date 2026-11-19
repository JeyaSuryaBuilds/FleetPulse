import { useCallback, useEffect, useMemo, useState } from 'react';
import { Plus, Pencil, Trash2, Search, ChevronLeft, ChevronRight, Upload } from 'lucide-react';
import { api, errorMessage } from '../../api.js';
import { useAuth } from '../../auth.jsx';
import { Button, EmptyState, ErrorNote, Field, Input, Modal, PageHeader, Select, Spinner, StatusBadge, Textarea, fmtDate, fmtDateTime, fmtMoney, useToast } from '../../ui.jsx';
import { useRefOptions, clearRefCache } from '../../useRefs.js';
import ShipmentRowActions from './ShipmentRowActions.jsx';

const toLocalInput = (v) => { if (!v) return ''; const d = new Date(v); if (isNaN(d)) return ''; const p = (n) => String(n).padStart(2, '0'); return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}`; };
const isGenerated = (f) => typeof f.hint === 'string' && f.hint.startsWith('Generated');

function RefSelect({ field, value, onChange }) {
  const { rows } = useRefOptions(field.ref.path, field.ref.labelKey);
  return (
    <Select value={value ?? ''} onChange={(e) => onChange(e.target.value === '' ? '' : Number(e.target.value))} required={field.req}>
      <option value="">{rows === null ? 'Loading…' : '— none —'}</option>
      {rows?.map((r) => <option key={r.id} value={r.id}>{r[field.ref.labelKey]}</option>)}
    </Select>
  );
}

function FileField({ field, value, onChange }) {
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState('');
  async function pick(e) {
    const file = e.target.files?.[0];
    if (!file) return;
    setBusy(true); setErr('');
    try { const fd = new FormData(); fd.append('file', file); fd.append('purpose', field.purpose || 'OTHER'); const { data } = await api.post('/files', fd); onChange(data.url); }
    catch (x) { setErr(errorMessage(x)); } finally { setBusy(false); }
  }
  return (
    <div>
      <label className="fp-btn fp-btn--secondary cursor-pointer"><Upload size={16} aria-hidden /> {busy ? 'Uploading…' : value ? 'Replace file' : 'Upload file'}<input type="file" className="hidden" accept="image/jpeg,image/png,image/webp,application/pdf" onChange={pick} /></label>
      {value && <span className="ml-3 text-sm text-fp-muted">Attached</span>}
      {err && <p className="mt-1 text-xs text-danger">{err}</p>}
    </div>
  );
}

function FieldInput({ field, value, onChange }) {
  switch (field.type) {
    case 'textarea': return <Textarea value={value ?? ''} onChange={(e) => onChange(e.target.value)} required={field.req} />;
    case 'number': return <Input type="number" step={field.step} value={value ?? ''} onChange={(e) => onChange(e.target.value)} required={field.req} />;
    case 'date': return <Input type="date" value={value ?? ''} onChange={(e) => onChange(e.target.value)} required={field.req} />;
    case 'datetime': return <Input type="datetime-local" value={toLocalInput(value)} onChange={(e) => onChange(e.target.value)} required={field.req} />;
    case 'bool': return <label className="flex items-center gap-2 py-2"><input type="checkbox" checked={!!value} onChange={(e) => onChange(e.target.checked)} /> Yes</label>;
    case 'select': return <Select value={value ?? ''} onChange={(e) => onChange(e.target.value)} required={field.req}><option value="">{field.req ? 'Select…' : '— none —'}</option>{field.options.map((o) => <option key={o} value={o}>{o.replaceAll('_', ' ')}</option>)}</Select>;
    case 'ref': return <RefSelect field={field} value={value} onChange={onChange} />;
    case 'file': return <FileField field={field} value={value} onChange={onChange} />;
    default: return <Input value={value ?? ''} onChange={(e) => onChange(e.target.value)} required={field.req} />;
  }
}

function Cell({ field, row, refLabels }) {
  const v = row[field.key];
  if (v === null || v === undefined || v === '') return <span className="text-fp-muted">—</span>;
  if (field.key === 'status' || field.key === 'priority' && false) return <StatusBadge value={v} />;
  switch (field.type) {
    case 'date': return fmtDate(v);
    case 'datetime': return fmtDateTime(v);
    case 'bool': return v ? 'Yes' : 'No';
    case 'ref': return refLabels[field.key]?.(v) ?? v;
    case 'select': return String(v).replaceAll('_', ' ');
    default: return field.money ? fmtMoney(v) : String(v);
  }
}

function RefLabelLoader({ field, onReady }) {
  const { label, rows } = useRefOptions(field.ref.path, field.ref.labelKey);
  useEffect(() => { if (rows) onReady(field.key, label); /* eslint-disable-next-line */ }, [rows]);
  return null;
}

export default function ResourcePage({ config }) {
  const { can } = useAuth();
  const toast = useToast();
  const [data, setData] = useState(null);
  const [page, setPage] = useState(0);
  const [q, setQ] = useState('');
  const [status, setStatus] = useState('');
  const [error, setError] = useState('');
  const [edit, setEdit] = useState(null);            // null | {} (new) | row
  const [form, setForm] = useState({});
  const [formErr, setFormErr] = useState({ message: '', fields: {} });
  const [saving, setSaving] = useState(false);
  const [del, setDel] = useState(null);
  const [refLabels, setRefLabels] = useState({});
  const writable = can(config.write);
  const fieldMap = useMemo(() => Object.fromEntries(config.fields.map((f) => [f.key, f])), [config]);
  const cols = useMemo(() => config.cols.map((k) => fieldMap[k] || { key: k, label: k.replace(/([A-Z])/g, ' $1').replace(/^./, (c) => c.toUpperCase()) }), [config, fieldMap]);
  const colMeta = useMemo(() => cols.map((c) => ({ ...c, label: c.label || c.key, type: c.type || (c.key === 'createdAt' ? 'datetime' : c.key === 'status' ? 'select' : c.key.endsWith('At') ? 'datetime' : 'text') })), [cols]);

  const load = useCallback(() => {
    setError('');
    const params = { page, size: 20, ...(q ? { q } : {}), ...(status ? { status } : {}) };
    api.get(`/${config.path}`, { params }).then((r) => setData(r.data)).catch((e) => { setData({ content: [], totalPages: 0 }); setError(errorMessage(e)); });
  }, [config.path, page, q, status]);

  useEffect(() => { setData(null); setPage(0); setQ(''); setStatus(''); }, [config.path]);
  useEffect(() => { load(); }, [load]);
  useEffect(() => { const t = setTimeout(() => setPage(0), 0); return () => clearTimeout(t); }, [q, status]);

  function openForm(row) {
    const initial = {};
    config.fields.forEach((f) => { initial[f.key] = row ? row[f.key] ?? '' : f.type === 'bool' ? (f.key === 'active' || f.key === 'publicVisible') : f.type === 'select' && f.req ? f.options[0] : ''; });
    setForm(initial); setFormErr({ message: '', fields: {} }); setEdit(row || {});
  }

  function payload() {
    const out = {};
    config.fields.forEach((f) => {
      if (f.readOnly) return;
      let v = form[f.key];
      if (v === '' || v === undefined) { if (isGenerated(f) || !edit?.id) return; v = null; }
      else if (f.type === 'number') v = Number(v);
      else if (f.type === 'datetime') v = new Date(v).toISOString();
      else if (f.type === 'ref') v = Number(v);
      out[f.key] = v;
    });
    return out;
  }

  async function save(e) {
    e.preventDefault();
    setSaving(true); setFormErr({ message: '', fields: {} });
    try {
      if (edit?.id) await api.patch(`/${config.path}/${edit.id}`, payload()); else await api.post(`/${config.path}`, payload());
      toast('Saved'); setEdit(null); clearRefCache(); load();
    } catch (err) { setFormErr({ message: errorMessage(err), fields: err?.response?.data?.fields || {} }); }
    finally { setSaving(false); }
  }

  async function remove() {
    try { await api.delete(`/${config.path}/${del.id}`); toast('Deleted'); setDel(null); clearRefCache(); load(); }
    catch (err) { toast(errorMessage(err), 'error'); setDel(null); }
  }

  async function quick(action, row) {
    try {
      if (action.patch) await api.patch(`/${config.path}/${row.id}`, action.patch); else await api.post(action.post(row));
      toast('Updated'); load();
    } catch (err) { toast(errorMessage(err), 'error'); }
  }

  const refFields = colMeta.filter((c) => c.type === 'ref');
  const canCreate = writable && !config.noCreate;

  return (
    <>
      {refFields.map((f) => <RefLabelLoader key={f.key} field={f} onReady={(k, fn) => setRefLabels((m) => (m[k] === fn ? m : { ...m, [k]: fn }))} />)}
      <PageHeader title={config.title} subtitle={config.note} actions={canCreate && <Button onClick={() => openForm(null)}><Plus size={16} aria-hidden /> New</Button>} />
      <div className="mb-4 flex flex-wrap gap-2">
        <div className="relative w-full max-w-xs"><Search size={16} className="absolute left-3 top-3 text-fp-muted" aria-hidden /><Input className="pl-9" placeholder="Search" aria-label="Search" value={q} onChange={(e) => setQ(e.target.value)} /></div>
        {config.statusOptions && <Select className="max-w-[12rem]" aria-label="Filter by status" value={status} onChange={(e) => setStatus(e.target.value)}><option value="">All statuses</option>{config.statusOptions.map((s) => <option key={s} value={s}>{s.replaceAll('_', ' ')}</option>)}</Select>}
      </div>
      <ErrorNote>{error}</ErrorNote>
      <div className="fp-card overflow-x-auto">
        {!data ? <Spinner /> : data.content.length === 0 ? <EmptyState title="Nothing here yet" text={canCreate ? 'Use “New” to add the first record.' : undefined} /> : (
          <table className="fp-table">
            <thead><tr>{colMeta.map((c) => <th key={c.key}>{c.label}</th>)}<th className="w-px" /></tr></thead>
            <tbody>
              {data.content.map((row) => (
                <tr key={row.id}>
                  {colMeta.map((c) => <td key={c.key}><Cell field={c} row={row} refLabels={refLabels} /></td>)}
                  <td className="whitespace-nowrap text-right">
                    {config.rowActions === 'shipment' && writable && <ShipmentRowActions row={row} reload={load} />}
                    {writable && config.quickActions?.filter((a) => a.when(row)).map((a) => <Button key={a.label} variant="ghost" onClick={() => quick(a, row)}>{a.label}</Button>)}
                    {writable && <Button variant="ghost" aria-label="Edit" onClick={() => openForm(row)}><Pencil size={15} /></Button>}
                    {writable && !config.noCreate && <Button variant="ghost" aria-label="Delete" onClick={() => setDel(row)}><Trash2 size={15} /></Button>}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
      {data && data.totalPages > 1 && (
        <div className="mt-4 flex items-center justify-end gap-2 text-sm text-fp-muted">
          <Button variant="ghost" disabled={page === 0} onClick={() => setPage(page - 1)} aria-label="Previous page"><ChevronLeft size={16} /></Button>
          Page {page + 1} of {data.totalPages}
          <Button variant="ghost" disabled={page + 1 >= data.totalPages} onClick={() => setPage(page + 1)} aria-label="Next page"><ChevronRight size={16} /></Button>
        </div>
      )}

      <Modal open={edit !== null} onClose={() => setEdit(null)} title={`${edit?.id ? 'Edit' : 'New'} ${config.title.toLowerCase()}`} wide
        footer={<><Button variant="ghost" onClick={() => setEdit(null)}>Cancel</Button><Button type="submit" form="resource-form" loading={saving}>Save</Button></>}>
        <form id="resource-form" onSubmit={save} className="grid gap-4 sm:grid-cols-2">
          {config.fields.map((f) => (
            <Field key={f.key} label={f.label + (f.req ? ' *' : '')} hint={f.hint} error={formErr.fields[f.key]} className={f.type === 'textarea' ? 'sm:col-span-2' : ''}>
              <FieldInput field={f} value={form[f.key]} onChange={(v) => setForm({ ...form, [f.key]: v })} />
            </Field>
          ))}
          <div className="sm:col-span-2"><ErrorNote>{formErr.message}</ErrorNote></div>
        </form>
      </Modal>

      <Modal open={!!del} onClose={() => setDel(null)} title="Delete record?" footer={<><Button variant="ghost" onClick={() => setDel(null)}>Cancel</Button><Button variant="danger" onClick={remove}>Delete</Button></>}>
        <p className="text-sm text-fp-muted">This cannot be undone. Records that are referenced elsewhere cannot be deleted.</p>
      </Modal>
    </>
  );
}
