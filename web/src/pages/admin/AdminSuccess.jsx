import { useState } from 'react';
import { api, errorMessage } from '../../api.js';
import { Button, EmptyState, ErrorNote, Field, Input, PageHeader, Select, Spinner, StatusBadge, Textarea, fmtDateTime, useToast } from '../../ui.jsx';
import { useGet } from './adminUtil.js';

const Tab = ({ id, cur, set, children }) => <button onClick={() => set(id)} aria-pressed={cur === id} className={`rounded-fp-sm px-4 py-1.5 text-sm ${cur === id ? 'bg-orange text-fp-on-orange font-medium' : 'text-fp-muted'}`}>{children}</button>;

export default function AdminSuccess() {
  const toast = useToast();
  const [tab, setTab] = useState('tickets');
  const [category, setCategory] = useState('');
  const tickets = useGet('/admin/support/tickets', { category: category || undefined, size: 50 });
  const contacts = useGet('/admin/contact-requests');
  const news = useGet('/admin/announcements');
  const [a, setA] = useState({ title: '', body: '', published: true });
  const [err, setErr] = useState('');

  async function ticket(id, body) { try { await api.patch(`/admin/support/tickets/${id}`, body); tickets.reload(); } catch (e) { setErr(errorMessage(e)); } }
  async function contact(id, status) { try { await api.patch(`/admin/contact-requests/${id}`, null, { params: { status } }); contacts.reload(); } catch (e) { setErr(errorMessage(e)); } }
  async function post(e) { e.preventDefault(); try { await api.post('/admin/announcements', a); toast('Announcement saved'); setA({ title: '', body: '', published: true }); news.reload(); } catch (x) { setErr(errorMessage(x)); } }
  async function toggle(n) { try { await api.put(`/admin/announcements/${n.id}`, { title: n.title, body: n.body, published: !n.published }); news.reload(); } catch (e) { setErr(errorMessage(e)); } }

  return (
    <>
      <PageHeader title="Customer success" subtitle="Support tickets, feedback, feature requests, bug reports, demo requests and announcements" />
      <ErrorNote>{err}</ErrorNote>
      <div className="mb-4 inline-flex rounded-fp-md border border-fp-border p-1"><Tab id="tickets" cur={tab} set={setTab}>Tickets</Tab><Tab id="contacts" cur={tab} set={setTab}>Website requests</Tab><Tab id="news" cur={tab} set={setTab}>Announcements</Tab></div>
      {tab === 'tickets' && (<>
        <Select className="mb-3 max-w-[14rem]" aria-label="Category" value={category} onChange={(e) => setCategory(e.target.value)}><option value="">All categories</option><option value="SUPPORT">Support</option><option value="BUG">Bug reports</option><option value="FEATURE">Feature requests</option><option value="FEEDBACK">Feedback</option></Select>
        <div className="fp-card overflow-x-auto">{!tickets.data ? <Spinner /> : tickets.data.content.length === 0 ? <EmptyState title="No tickets" /> : (
          <table className="fp-table"><thead><tr><th>Date</th><th>Company</th><th>Category</th><th>Subject</th><th>Priority</th><th>Status</th></tr></thead>
            <tbody>{tickets.data.content.map((t) => <tr key={t.id}><td>{fmtDateTime(t.createdAt)}</td><td>{t.company}</td><td>{t.category}</td><td title={t.description}>{t.subject}</td><td>{t.priority}</td>
              <td><Select aria-label="Ticket status" value={t.status} onChange={(e) => ticket(t.id, { status: e.target.value })}>{['OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'].map((s) => <option key={s}>{s}</option>)}</Select></td></tr>)}</tbody></table>)}</div></>)}
      {tab === 'contacts' && <div className="fp-card overflow-x-auto">{!contacts.data ? <Spinner /> : contacts.data.length === 0 ? <EmptyState title="No requests" /> : (
        <table className="fp-table"><thead><tr><th>Date</th><th>Type</th><th>Name</th><th>Email</th><th>Company</th><th>Message</th><th>Status</th></tr></thead>
          <tbody>{contacts.data.map((c) => <tr key={c.id}><td>{fmtDateTime(c.createdAt)}</td><td>{c.kind}</td><td>{c.name}</td><td><a className="text-orange underline" href={`mailto:${c.email}`}>{c.email}</a></td><td>{c.companyName || '—'}</td><td className="max-w-xs truncate" title={c.message}>{c.message || '—'}</td>
            <td><Select aria-label="Request status" value={c.status} onChange={(e) => contact(c.id, e.target.value)}>{['NEW', 'CONTACTED', 'CLOSED'].map((s) => <option key={s}>{s}</option>)}</Select></td></tr>)}</tbody></table>)}</div>}
      {tab === 'news' && (<>
        <form onSubmit={post} className="fp-card fp-card--padded mb-4 max-w-2xl space-y-3"><Field label="Title"><Input required maxLength={200} value={a.title} onChange={(e) => setA({ ...a, title: e.target.value })} /></Field><Field label="Message"><Textarea required value={a.body} onChange={(e) => setA({ ...a, body: e.target.value })} /></Field>
          <label className="flex items-center gap-2 text-sm"><input type="checkbox" checked={a.published} onChange={(e) => setA({ ...a, published: e.target.checked })} /> Publish to all companies now</label><Button type="submit">Save announcement</Button></form>
        <div className="space-y-2">{news.data?.map((n) => <div key={n.id} className="fp-card fp-card--padded flex items-start justify-between gap-3"><div><p className="font-medium">{n.title} <StatusBadge value={n.published ? 'ACTIVE' : 'DRAFT'} /></p><p className="text-sm text-fp-muted">{n.body}</p></div><Button variant="ghost" onClick={() => toggle(n)}>{n.published ? 'Unpublish' : 'Publish'}</Button></div>)}</div></>)}
    </>
  );
}
