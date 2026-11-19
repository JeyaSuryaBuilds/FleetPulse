import { useEffect, useRef, useState } from 'react';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { api, errorMessage } from '../../api.js';
import { EmptyState, ErrorNote, PageHeader, StatusBadge, fmtDateTime } from '../../ui.jsx';

/**
 * Shows only positions actually reported by driver apps. A vehicle with no report, or none in the last 10 minutes, is listed as
 * "no live signal" and is never drawn at an invented location. Tiles: OpenStreetMap (respect the tile usage policy; use your own provider at scale).
 */
export default function LiveMap() {
  const mapEl = useRef(null);
  const map = useRef(null);
  const layer = useRef(null);
  const trail = useRef(null);
  const [vehicles, setVehicles] = useState(null);
  const [selected, setSelected] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    map.current = L.map(mapEl.current, { zoomControl: true }).setView([20, 0], 2);
    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19, attribution: '&copy; OpenStreetMap contributors' }).addTo(map.current);
    layer.current = L.layerGroup().addTo(map.current);
    return () => map.current.remove();
  }, []);

  useEffect(() => {
    let first = true;
    const tick = () => api.get('/gps/live').then(({ data }) => {
      setVehicles(data); setError('');
      layer.current.clearLayers();
      const pts = [];
      data.filter((v) => v.latitude != null && !v.stale).forEach((v) => {
        const ll = [Number(v.latitude), Number(v.longitude)]; pts.push(ll);
        L.circleMarker(ll, { radius: 9, color: '#111111', weight: 2, fillColor: '#FF8733', fillOpacity: 1 })
          .bindTooltip(`${v.registrationNumber}${v.speedKmh != null ? ` · ${Math.round(v.speedKmh)} km/h` : ''}`).on('click', () => setSelected(v.vehicleId)).addTo(layer.current);
      });
      if (first && pts.length) { map.current.fitBounds(pts, { padding: [40, 40], maxZoom: 14 }); first = false; }
    }).catch((e) => setError(errorMessage(e)));
    tick();
    const t = setInterval(tick, 15000);
    return () => clearInterval(t);
  }, []);

  useEffect(() => {
    if (trail.current) { trail.current.remove(); trail.current = null; }
    if (!selected) return;
    const to = new Date(), from = new Date(Date.now() - 24 * 3600 * 1000);
    api.get('/gps/history', { params: { vehicleId: selected, from: from.toISOString(), to: to.toISOString() } }).then(({ data }) => {
      if (data.length < 2) return;
      trail.current = L.polyline(data.map((p) => [Number(p.latitude), Number(p.longitude)]), { color: '#F97316', weight: 3, opacity: 0.8 }).addTo(map.current);
    }).catch(() => {});
  }, [selected]);

  const live = vehicles?.filter((v) => v.latitude != null && !v.stale) || [];
  const noSignal = vehicles?.filter((v) => v.latitude == null || v.stale) || [];
  return (
    <>
      <PageHeader title="Live map" subtitle="Real positions reported by driver apps. Updates every 15 seconds." />
      <ErrorNote>{error}</ErrorNote>
      <div className="grid gap-4 lg:grid-cols-[1fr_320px]">
        <div className="fp-card overflow-hidden"><div ref={mapEl} style={{ height: 560 }} aria-label="Map of live vehicle positions" /></div>
        <div className="fp-card fp-card--padded max-h-[560px] overflow-y-auto">
          <h2 className="mb-2 font-medium">Reporting now ({live.length})</h2>
          {vehicles && live.length === 0 && <p className="text-sm text-fp-muted">No vehicle has reported a position in the last 10 minutes.</p>}
          <ul className="space-y-2">{live.map((v) => (
            <li key={v.vehicleId}><button className={`w-full rounded-fp-md border px-3 py-2 text-left text-sm ${selected === v.vehicleId ? 'border-orange' : 'border-fp-border'}`} onClick={() => { setSelected(v.vehicleId); map.current.setView([Number(v.latitude), Number(v.longitude)], 15); }}>
              <span className="font-medium">{v.registrationNumber}</span> <StatusBadge value={v.vehicleStatus} /><br /><span className="text-xs text-fp-muted">{fmtDateTime(v.recordedAt)}</span></button></li>))}</ul>
          <h2 className="mb-2 mt-5 font-medium">No live signal ({noSignal.length})</h2>
          {noSignal.length === 0 && vehicles && <p className="text-sm text-fp-muted">All active vehicles are reporting.</p>}
          <ul className="space-y-1 text-sm text-fp-muted">{noSignal.map((v) => <li key={v.vehicleId}>{v.registrationNumber}{v.recordedAt ? ` · last seen ${fmtDateTime(v.recordedAt)}` : ' · never reported'}</li>)}</ul>
          {vehicles && vehicles.length === 0 && <EmptyState title="No vehicles yet" text="Add vehicles and assign drivers using the mobile app." />}
        </div>
      </div>
    </>
  );
}
