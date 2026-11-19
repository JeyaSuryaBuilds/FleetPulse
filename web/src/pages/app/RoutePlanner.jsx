import { useEffect, useRef, useState } from 'react';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { Plus, Trash2 } from 'lucide-react';
import { api, errorMessage } from '../../api.js';
import { Button, ErrorNote, Field, Input, PageHeader, Select, useToast, fmtDateTime, fmtMoney } from '../../ui.jsx';
import { useRefOptions } from '../../useRefs.js';

const pt = () => ({ lat: '', lng: '' });
const valid = (p) => p.lat !== '' && p.lng !== '' && !isNaN(Number(p.lat)) && !isNaN(Number(p.lng));

export default function RoutePlanner() {
  const toast = useToast();
  const mapEl = useRef(null), map = useRef(null), line = useRef(null);
  const vehicles = useRefOptions('vehicles', 'registrationNumber');
  const [origin, setOrigin] = useState(pt());
  const [dest, setDest] = useState(pt());
  const [stops, setStops] = useState([]);
  const [mode, setMode] = useState('FASTEST');
  const [vehicleId, setVehicleId] = useState('');
  const [toll, setToll] = useState('');
  const [res, setRes] = useState(null);
  const [busy, setBusy] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    map.current = L.map(mapEl.current).setView([20, 0], 2);
    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19, attribution: '&copy; OpenStreetMap contributors' }).addTo(map.current);
    return () => map.current.remove();
  }, []);

  const body = (extra = {}) => ({ origin: { lat: Number(origin.lat), lng: Number(origin.lng) }, ...extra });
  const num = (p) => ({ lat: Number(p.lat), lng: Number(p.lng) });

  async function plan() {
    setBusy('plan'); setError(''); setRes(null);
    try {
      const { data } = await api.post('/route-planning/plan', { ...body({ destination: num(dest), stops: stops.filter(valid).map(num), mode, vehicleId: vehicleId ? Number(vehicleId) : null, tollRatePerKm: toll ? Number(toll) : null }) });
      setRes(data);
      if (line.current) line.current.remove();
      line.current = L.polyline(data.geometry, { color: '#FF8733', weight: 4 }).addTo(map.current);
      map.current.fitBounds(line.current.getBounds(), { padding: [30, 30] });
    } catch (e) { setError(errorMessage(e)); } finally { setBusy(''); }
  }

  async function optimize() {
    setBusy('opt'); setError('');
    try {
      const list = stops.filter(valid);
      const { data } = await api.post('/route-planning/optimize', { ...body({ stops: list.map(num) }), returnToOrigin: false });
      setStops(data.stopOrder.map((i) => list[i]));
      toast(`Stops reordered: ${data.totalDriveMinutesAsGiven} → ${data.totalDriveMinutesOptimized} min driving`);
    } catch (e) { setError(errorMessage(e)); } finally { setBusy(''); }
  }

  async function save() {
    try {
      await api.post('/routes', { name: `Route ${new Date().toLocaleDateString()}`, mode, distanceKm: res.distanceKm, durationMinutes: res.durationMinutes, tollEstimate: res.tollEstimate, fuelEstimateLitres: res.fuelEstimateLitres, waypointsJson: JSON.stringify(res.geometry), computedBy: 'OpenRouteService' });
      toast('Route saved');
    } catch (e) { toast(errorMessage(e), 'error'); }
  }

  const Pt = ({ label, v, set }) => (
    <div className="grid grid-cols-2 gap-2"><Field label={`${label} latitude`}><Input type="number" step="any" value={v.lat} onChange={(e) => set({ ...v, lat: e.target.value })} /></Field><Field label={`${label} longitude`}><Input type="number" step="any" value={v.lng} onChange={(e) => set({ ...v, lng: e.target.value })} /></Field></div>
  );

  return (
    <>
      <PageHeader title="Route planner" subtitle="Real road routing (OpenRouteService). Enter coordinates for each point." />
      <div className="grid gap-4 lg:grid-cols-[380px_1fr]">
        <div className="fp-card fp-card--padded space-y-3">
          <Pt label="Origin" v={origin} set={setOrigin} />
          {stops.map((s, i) => (
            <div key={i} className="flex items-end gap-2"><div className="flex-1"><Pt label={`Stop ${i + 1}`} v={s} set={(v) => setStops(stops.map((x, j) => (j === i ? v : x)))} /></div><Button variant="ghost" aria-label="Remove stop" onClick={() => setStops(stops.filter((_, j) => j !== i))}><Trash2 size={16} /></Button></div>
          ))}
          <Button variant="secondary" onClick={() => setStops([...stops, pt()])} disabled={stops.length >= 20}><Plus size={16} aria-hidden /> Add stop</Button>
          <Pt label="Destination" v={dest} set={setDest} />
          <Field label="Mode"><Select value={mode} onChange={(e) => setMode(e.target.value)}><option value="FASTEST">Fastest</option><option value="SHORTEST">Shortest</option><option value="FUEL_EFFICIENT">Fuel-efficient (approximation)</option></Select></Field>
          <Field label="Vehicle (for fuel estimate)"><Select value={vehicleId} onChange={(e) => setVehicleId(e.target.value)}><option value="">— none —</option>{vehicles.rows?.map((v) => <option key={v.id} value={v.id}>{v.registrationNumber}</option>)}</Select></Field>
          <Field label="Toll rate per km (optional)" hint="Toll prices are not in the map data; set your own rate to estimate."><Input type="number" step="any" value={toll} onChange={(e) => setToll(e.target.value)} /></Field>
          <ErrorNote>{error}</ErrorNote>
          <div className="flex flex-wrap gap-2"><Button loading={busy === 'plan'} disabled={!valid(origin) || !valid(dest)} onClick={plan}>Plan route</Button><Button variant="secondary" loading={busy === 'opt'} disabled={!valid(origin) || stops.filter(valid).length < 2} onClick={optimize}>Optimise stop order</Button></div>
        </div>
        <div className="space-y-4">
          <div className="fp-card overflow-hidden"><div ref={mapEl} style={{ height: 420 }} /></div>
          {res && (
            <div className="fp-card fp-card--padded text-sm">
              <div className="grid gap-3 sm:grid-cols-4"><div><p className="text-xs text-fp-muted">Distance</p><p className="text-lg font-semibold">{res.distanceKm} km</p></div><div><p className="text-xs text-fp-muted">Drive time</p><p className="text-lg font-semibold">{res.durationMinutes} min</p></div><div><p className="text-xs text-fp-muted">ETA if leaving now</p><p className="text-lg font-semibold">{fmtDateTime(res.etaAt)}</p></div><div><p className="text-xs text-fp-muted">Toll roads</p><p className="text-lg font-semibold">{res.tollRoadKm} km</p></div></div>
              <p className="mt-3 text-fp-muted">Fuel: {res.fuelEstimateLitres != null ? `${res.fuelEstimateLitres} L. ` : ''}{res.fuelEstimateNote}</p>
              <p className="text-fp-muted">Toll: {res.tollEstimate != null ? `${fmtMoney(res.tollEstimate)}. ` : ''}{res.tollEstimateNote}</p>
              {res.note && <p className="text-fp-muted">{res.note}</p>}
              <Button className="mt-3" variant="secondary" onClick={save}>Save route</Button>
            </div>)}
        </div>
      </div>
    </>
  );
}
