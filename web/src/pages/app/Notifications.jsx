import { useEffect, useState } from 'react';
import { api, errorMessage } from '../../api.js';
import { Badge, Button, EmptyState, ErrorNote, PageHeader, Spinner, fmtDateTime } from '../../ui.jsx';

export default function Notifications() {
  const [rows, setRows] = useState(null);
  const [error, setError] = useState('');
  const [news, setNews] = useState([]);
  const load = () => api.get('/me/notifications').then((r) => setRows(r.data)).catch((e) => setError(errorMessage(e)));
  useEffect(() => { load(); api.get('/company/announcements').then((r) => setNews(r.data)).catch(() => {}); }, []);
  async function read(id) { await api.post(`/me/notifications/${id}/read`).catch(() => {}); load(); }
  return (
    <>
      <PageHeader title="Notifications" subtitle="Vehicle, driver, shipment, maintenance, licence, insurance and payment alerts" />
      {news.map((a) => <div key={a.id} className="fp-card fp-card--padded mb-3 border-orange"><p className="font-medium">{a.title}</p><p className="mt-1 text-sm text-fp-muted">{a.body}</p></div>)}
      <ErrorNote>{error}</ErrorNote>
      {!rows ? <Spinner /> : rows.length === 0 ? <div className="fp-card"><EmptyState title="No notifications" /></div> : (
        <ul className="space-y-2">{rows.map((n) => (
          <li key={n.id} className={`fp-card fp-card--padded flex items-start justify-between gap-3 ${n.readAt ? 'opacity-70' : ''}`}>
            <div><div className="flex items-center gap-2"><p className="font-medium">{n.title}</p><Badge tone={n.severity === 'WARNING' ? 'warning' : n.severity === 'ERROR' ? 'error' : 'brand'}>{n.type.replaceAll('_', ' ')}</Badge></div>
              {n.message && <p className="mt-1 text-sm text-fp-muted">{n.message}</p>}<p className="mt-1 text-xs text-fp-muted">{fmtDateTime(n.createdAt)}</p></div>
            {!n.readAt && <Button variant="ghost" onClick={() => read(n.id)}>Mark read</Button>}
          </li>))}</ul>)}
    </>
  );
}
