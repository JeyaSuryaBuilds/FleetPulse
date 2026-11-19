import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { MapPin, CheckCircle2, Circle } from 'lucide-react';
import { api, errorMessage } from '../../api.js';
import { Button, ErrorNote, Field, Input, Spinner, StatusBadge, fmtDateTime } from '../../ui.jsx';

export default function Track() {
  const { id } = useParams();
  const nav = useNavigate();
  const [code, setCode] = useState(id || '');
  const [data, setData] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!id) { setData(null); return; }
    setLoading(true); setError('');
    api.get(`/public/tracking/${encodeURIComponent(id)}`).then((r) => setData(r.data)).catch((e) => { setData(null); setError(e?.response?.status === 404 ? 'No shipment found with this tracking ID.' : errorMessage(e)); }).finally(() => setLoading(false));
  }, [id]);

  return (
    <section className="mx-auto max-w-2xl px-4 py-16 sm:px-6">
      <h1 className="text-4xl font-semibold tracking-tight">Track your shipment</h1>
      <form className="mt-6 flex items-end gap-2" onSubmit={(e) => { e.preventDefault(); if (code.trim()) nav(`/track/${code.trim()}`); }}>
        <Field label="Tracking ID" className="flex-1"><Input value={code} onChange={(e) => setCode(e.target.value)} placeholder="e.g. FPAB12CD34EF" autoFocus /></Field>
        <Button type="submit">Track</Button>
      </form>
      {loading && <Spinner />}
      <div className="mt-4"><ErrorNote>{error}</ErrorNote></div>
      {data && (
        <div className="fp-card fp-card--padded mt-6 space-y-5">
          <div className="flex items-center justify-between"><div><p className="text-xs text-fp-muted">Tracking ID</p><p className="font-mono text-lg">{data.trackingId}</p></div><StatusBadge value={data.status} /></div>
          <div className="grid gap-4 sm:grid-cols-2">
            <div><p className="text-xs text-fp-muted">From</p><p>{data.pickup}</p></div>
            <div><p className="text-xs text-fp-muted">To</p><p>{data.destination}</p></div>
            <div><p className="text-xs text-fp-muted">Estimated delivery</p><p>{fmtDateTime(data.estimatedDelivery)}</p></div>
            <div><p className="text-xs text-fp-muted">Shipped by</p><p>{data.company}</p></div>
          </div>
          <div className="rounded-fp-md border border-fp-border bg-fp-inset p-4">
            <p className="flex items-center gap-2 text-sm font-medium"><MapPin size={16} className="text-orange" aria-hidden />Current location</p>
            {data.locationAvailable ? (
              <p className="mt-1 text-sm text-fp-muted">Last reported {fmtDateTime(data.currentLocation.recordedAt)}: {Number(data.currentLocation.latitude).toFixed(5)}, {Number(data.currentLocation.longitude).toFixed(5)}{' '}
                <a className="text-orange underline" target="_blank" rel="noreferrer noopener" href={`https://www.openstreetmap.org/?mlat=${data.currentLocation.latitude}&mlon=${data.currentLocation.longitude}#map=15/${data.currentLocation.latitude}/${data.currentLocation.longitude}`}>View on map</a></p>
            ) : <p className="mt-1 text-sm text-fp-muted">Live location is not available for this shipment right now.</p>}
          </div>
          <div>
            <p className="mb-3 text-sm font-medium">Timeline</p>
            {data.timeline.length === 0 ? <p className="text-sm text-fp-muted">No updates yet.</p> : (
              <ol className="space-y-3">
                {data.timeline.map((t, i) => (
                  <li key={i} className="flex gap-3">{i === data.timeline.length - 1 ? <CheckCircle2 size={18} className="mt-0.5 text-orange" aria-hidden /> : <Circle size={18} className="mt-0.5 text-fp-muted" aria-hidden />}
                    <div><p className="text-sm font-medium">{t.status.replaceAll('_', ' ')}</p>{t.note && <p className="text-sm text-fp-muted">{t.note}</p>}<p className="text-xs text-fp-muted">{fmtDateTime(t.occurredAt)}</p></div></li>
                ))}
              </ol>
            )}
          </div>
          {data.status === 'DELIVERED' && <Feedback id={data.trackingId} />}
        </div>
      )}
    </section>
  );
}

function Feedback({ id }) {
  const [rating, setRating] = useState(0);
  const [comment, setComment] = useState('');
  const [state, setState] = useState({ done: false, error: '' });
  async function send() {
    try { await api.post(`/public/tracking/${id}/feedback`, { rating, comment, kind: 'FEEDBACK' }); setState({ done: true, error: '' }); }
    catch (e) { setState({ done: false, error: errorMessage(e) }); }
  }
  if (state.done) return <p className="text-sm text-success">Thank you for your feedback.</p>;
  return (
    <div className="border-t border-fp-border pt-4">
      <p className="mb-2 text-sm font-medium">How was your delivery?</p>
      <div className="mb-2 flex gap-1">{[1, 2, 3, 4, 5].map((n) => <button key={n} aria-label={`${n} stars`} onClick={() => setRating(n)} className={`h-9 w-9 rounded-fp-sm border ${rating >= n ? 'border-orange text-orange' : 'border-fp-border text-fp-muted'}`}>{n}</button>)}</div>
      <Input value={comment} maxLength={2000} onChange={(e) => setComment(e.target.value)} placeholder="Optional comment" />
      <ErrorNote>{state.error}</ErrorNote>
      <Button className="mt-2" disabled={!rating} onClick={send}>Send feedback</Button>
    </div>
  );
}
