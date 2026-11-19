import { useCallback, useEffect, useState } from 'react';
import { api, errorMessage } from '../../api.js';
import { useAuth } from '../../auth.jsx';
import { Button, EmptyState, ErrorNote, Field, Input, PageHeader, Spinner, StatusBadge, fmtMoney, useToast } from '../../ui.jsx';

export default function PayrollRuns() {
  const { can } = useAuth();
  const toast = useToast();
  const [runs, setRuns] = useState(null);
  const [month, setMonth] = useState(new Date().toISOString().slice(0, 7));
  const [error, setError] = useState('');
  const [busy, setBusy] = useState('');
  const write = can('PAYROLL_WRITE');
  const load = useCallback(() => api.get('/payroll-runs', { params: { size: 50, sort: 'periodMonth,desc' } }).then((r) => setRuns(r.data.content)).catch((e) => setError(errorMessage(e))), []);
  useEffect(() => { load(); }, [load]);

  async function act(key, fn, ok) {
    setBusy(key); setError('');
    try { await fn(); toast(ok); load(); } catch (e) { setError(errorMessage(e)); } finally { setBusy(''); }
  }

  return (
    <>
      <PageHeader title="Payroll runs" subtitle="Generate, finalise and pay. Fixed-salary and work-based employees are handled together." />
      {write && (
        <div className="fp-card fp-card--padded mb-4 flex flex-wrap items-end gap-3">
          <Field label="Period"><Input type="month" value={month} onChange={(e) => setMonth(e.target.value)} /></Field>
          <Button loading={busy === 'gen'} onClick={() => act('gen', () => api.post(`/payroll/generate/${month}`), 'Payslips generated')}>Generate / refresh draft</Button>
        </div>)}
      <p className="mb-3 text-xs text-fp-muted">Assumptions: working days are Monday to Friday, a standard day is 8 hours, absent days are deducted at monthly salary ÷ working days, advances are recovered in full, and work-based pay includes only APPROVED work records. Review the draft payslips before finalising.</p>
      <ErrorNote>{error}</ErrorNote>
      <div className="fp-card overflow-x-auto">{!runs ? <Spinner /> : runs.length === 0 ? <EmptyState title="No payroll runs yet" /> : (
        <table className="fp-table"><thead><tr><th>Period</th><th>Total net</th><th>Status</th><th /></tr></thead>
          <tbody>{runs.map((r) => (
            <tr key={r.id}><td>{r.periodMonth}</td><td>{fmtMoney(r.totalNet)}</td><td><StatusBadge value={r.status} /></td>
              <td className="text-right">{write && r.status === 'DRAFT' && <Button variant="secondary" loading={busy === `f${r.id}`} onClick={() => act(`f${r.id}`, () => api.post(`/payroll/runs/${r.id}/finalize`), 'Run finalised. Payslips are now visible to employees.')}>Finalise</Button>}
                {write && r.status === 'FINALIZED' && <Button loading={busy === `p${r.id}`} onClick={() => act(`p${r.id}`, () => api.post(`/payroll/runs/${r.id}/mark-paid`), 'Marked as paid and salary expenses booked')}>Mark paid</Button>}</td></tr>))}</tbody></table>)}</div>
    </>
  );
}
