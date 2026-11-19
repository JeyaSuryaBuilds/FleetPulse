import { useEffect, useState } from 'react';
import { api, errorMessage } from '../../api.js';
import { Badge, EmptyState, ErrorNote, PageHeader, Spinner, fmtMoney } from '../../ui.jsx';

function useGet(path, enabled = true) {
  const [s, setS] = useState({ data: null, error: '' });
  useEffect(() => { if (enabled) api.get(path).then((r) => setS({ data: r.data, error: '' })).catch((e) => setS({ data: [], error: errorMessage(e) })); }, [path, enabled]);
  return s;
}

export default function Analytics() {
  const dep = useGet('/analytics/depreciation');
  const util = useGet('/analytics/utilization');
  const fuel = useGet('/analytics/fuel-efficiency');
  return (
    <>
      <PageHeader title="Fleet analytics" subtitle="Calculated from recorded data. Vehicles with missing inputs are listed as not calculable, never estimated." />

      <h2 className="mb-2 font-medium">Fleet utilisation (last 30 days)</h2>
      <ErrorNote>{util.error}</ErrorNote>
      <div className="fp-card mb-8 overflow-x-auto">{!util.data ? <Spinner /> : !util.data.vehicles?.length ? <EmptyState title="No vehicles" /> : (<>
        <table className="fp-table"><thead><tr><th>Vehicle</th><th>Active days</th><th>Utilisation</th><th>Distance (km)</th></tr></thead>
          <tbody>{util.data.vehicles.map((v) => <tr key={v.vehicleId}><td>{v.registrationNumber}</td><td>{v.activeDays}</td><td>{v.utilizationPercent}%</td><td>{Number(v.distanceKm).toFixed(1)}</td></tr>)}</tbody></table>
        <p className="px-4 py-3 text-xs text-fp-muted">Fleet average {util.data.fleetAveragePercent}%. Utilisation = days with at least one started journey ÷ days in the period.</p></>)}</div>

      <h2 className="mb-2 font-medium">Fuel efficiency (last 90 days)</h2>
      <ErrorNote>{fuel.error}</ErrorNote>
      {!fuel.data ? <Spinner /> : (<>
        <div className="fp-card mb-4 overflow-x-auto">{fuel.data.vehicles?.length ? (
          <table className="fp-table"><thead><tr><th>Vehicle</th><th>Distance (km)</th><th>Litres</th><th>km / L</th><th>L / 100 km</th></tr></thead>
            <tbody>{fuel.data.vehicles.map((v) => <tr key={v.vehicleId}><td>{v.registrationNumber}</td><td>{v.distanceKm}</td><td>{v.litres}</td><td>{v.kmPerLitre}</td><td>{v.litresPer100Km}</td></tr>)}</tbody></table>) : <EmptyState title="Not enough data" text="Efficiency needs fuel records that include odometer readings." />}</div>
        {fuel.data.unusualConsumption?.length > 0 && (<div className="fp-card fp-card--padded mb-4"><h3 className="mb-2 flex items-center gap-2 font-medium">Unusual consumption <Badge tone="warning">{fuel.data.unusualConsumption.length}</Badge></h3>
          <ul className="space-y-2 text-sm">{fuel.data.unusualConsumption.map((a) => <li key={a.fuelRecordId}><b>{a.registrationNumber}</b> on {a.fuelDate}: {a.kmPerLitre} km/L vs average {a.vehicleAverageKmPerLitre}. <span className="text-fp-muted">{a.reason}</span></li>)}</ul></div>)}
        {fuel.data.insufficientData?.length > 0 && <p className="mb-8 text-xs text-fp-muted">Not enough odometer readings: {fuel.data.insufficientData.map((v) => v.registrationNumber).join(', ')}. Method: {fuel.data.method}.</p>}
      </>)}

      <h2 className="mb-2 mt-6 font-medium">Depreciation and resale</h2>
      <ErrorNote>{dep.error}</ErrorNote>
      <div className="fp-card overflow-x-auto">{!dep.data ? <Spinner /> : dep.data.length === 0 ? <EmptyState title="No vehicles" /> : (
        <table className="fp-table"><thead><tr><th>Vehicle</th><th>Annual depreciation</th><th>Accumulated</th><th>Book value</th><th>Resale gain / loss</th></tr></thead>
          <tbody>{dep.data.map((v) => v.calculable ? <tr key={v.vehicleId}><td>{v.registrationNumber}</td><td>{fmtMoney(v.annualDepreciation)}</td><td>{fmtMoney(v.accumulatedDepreciation)}</td><td>{fmtMoney(v.currentBookValue)}</td><td>{v.resaleGainLoss != null ? fmtMoney(v.resaleGainLoss) : '—'}</td></tr>
            : <tr key={v.vehicleId}><td>{v.registrationNumber}</td><td colSpan={4} className="text-fp-muted">{v.missing}</td></tr>)}</tbody></table>)}</div>
    </>
  );
}
