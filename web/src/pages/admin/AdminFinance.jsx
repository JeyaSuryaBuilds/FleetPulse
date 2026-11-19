import { useState } from 'react';
import { api, errorMessage } from '../../api.js';
import { Button, EmptyState, ErrorNote, PageHeader, Select, Spinner, StatusBadge, fmtDateTime, fmtMoney, useToast } from '../../ui.jsx';
import { useGet } from './adminUtil.js';

const Tab = ({ id, cur, set, children }) => <button onClick={() => set(id)} aria-pressed={cur === id} className={`rounded-fp-sm px-4 py-1.5 text-sm ${cur === id ? 'bg-orange text-fp-on-orange font-medium' : 'text-fp-muted'}`}>{children}</button>;

export default function AdminFinance() {
  const toast = useToast();
  const [tab, setTab] = useState('payments');
  const [status, setStatus] = useState('');
  const pay = useGet('/admin/finance/payments', { status: status || undefined, size: 50 });
  const inv = useGet('/admin/finance/invoices');
  const tax = useGet('/admin/finance/tax-report');
  async function refund(id) {
    if (!window.confirm('Record a refund request? The money must then be refunded in the payment provider dashboard.')) return;
    try { await api.post(`/admin/finance/payments/${id}/refund-request`); toast('Refund request recorded'); pay.reload(); } catch (e) { toast(errorMessage(e), 'error'); }
  }
  return (
    <>
      <PageHeader title="Financial management" subtitle="Figures come from payment-provider webhooks. Until payments are configured these lists are empty." />
      <div className="mb-4 inline-flex rounded-fp-md border border-fp-border p-1"><Tab id="payments" cur={tab} set={setTab}>Payments</Tab><Tab id="invoices" cur={tab} set={setTab}>Invoices</Tab><Tab id="tax" cur={tab} set={setTab}>Tax report</Tab></div>
      {tab === 'payments' && (<>
        <Select className="mb-3 max-w-[12rem]" aria-label="Status" value={status} onChange={(e) => setStatus(e.target.value)}><option value="">All</option><option>SUCCEEDED</option><option>FAILED</option><option>REFUND_REQUESTED</option><option>REFUNDED</option></Select>
        <ErrorNote>{pay.error}</ErrorNote>
        <div className="fp-card overflow-x-auto">{!pay.data ? <Spinner /> : pay.data.content.length === 0 ? <EmptyState title="No payments" /> : (
          <table className="fp-table"><thead><tr><th>Date</th><th>Company</th><th>Amount</th><th>Status</th><th>Gateway ref</th><th>Failure</th><th /></tr></thead>
            <tbody>{pay.data.content.map((p) => <tr key={p.id}><td>{fmtDateTime(p.createdAt)}</td><td>{p.company}</td><td>{fmtMoney(p.amount, p.currency)}</td><td><StatusBadge value={p.status} /></td><td>{p.gatewayRef || '—'}</td><td>{p.failureReason || '—'}</td><td>{p.status === 'SUCCEEDED' && <Button variant="ghost" onClick={() => refund(p.id)}>Request refund</Button>}</td></tr>)}</tbody></table>)}</div></>)}
      {tab === 'invoices' && <div className="fp-card overflow-x-auto">{!inv.data ? <Spinner /> : inv.data.length === 0 ? <EmptyState title="No invoices" /> : (
        <table className="fp-table"><thead><tr><th>Invoice</th><th>Company</th><th>Total</th><th>Tax</th><th>Status</th><th>Issued</th></tr></thead><tbody>{inv.data.map((i) => <tr key={i.id}><td>{i.invoiceNumber}</td><td>{i.company}</td><td>{fmtMoney(i.total, i.currency)}</td><td>{fmtMoney(i.tax, i.currency)}</td><td><StatusBadge value={i.status} /></td><td>{fmtDateTime(i.issuedAt)}</td></tr>)}</tbody></table>)}</div>}
      {tab === 'tax' && <div className="fp-card overflow-x-auto">{!tax.data ? <Spinner /> : tax.data.length === 0 ? <EmptyState title="No tax data" /> : (
        <table className="fp-table"><thead><tr><th>Month</th><th>Currency</th><th>Subtotal</th><th>Tax</th><th>Total</th><th>Invoices</th></tr></thead><tbody>{tax.data.map((t, i) => <tr key={i}><td>{t.month}</td><td>{t.currency}</td><td>{Number(t.subtotal).toFixed(2)}</td><td>{Number(t.tax).toFixed(2)}</td><td>{Number(t.total).toFixed(2)}</td><td>{t.invoices}</td></tr>)}</tbody></table>)}</div>}
    </>
  );
}
