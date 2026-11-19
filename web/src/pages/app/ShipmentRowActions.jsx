import { useState } from 'react';
import { api, errorMessage } from '../../api.js';
import { Button, ErrorNote, Field, Input, Modal, Select, useToast } from '../../ui.jsx';
import { useRefOptions } from '../../useRefs.js';

// Mirrors the backend transition rules (ShipmentWorkflowService); the server remains the authority.
const NEXT = {
  CREATED: ['CANCELLED'], ASSIGNED: ['PICKED_UP', 'CANCELLED'], PICKED_UP: ['IN_TRANSIT', 'FAILED'], IN_TRANSIT: ['OUT_FOR_DELIVERY', 'DELIVERED', 'FAILED'],
  OUT_FOR_DELIVERY: ['DELIVERED', 'FAILED'], FAILED: ['RETURNED'],
};

export default function ShipmentRowActions({ row, reload }) {
  const toast = useToast();
  const [mode, setMode] = useState(null);
  const [f, setF] = useState({});
  const [err, setErr] = useState('');
  const [busy, setBusy] = useState(false);
  const drivers = useRefOptions(mode === 'assign' ? 'drivers' : null, 'fullName');
  const vehicles = useRefOptions(mode === 'assign' ? 'vehicles' : null, 'registrationNumber');
  const next = NEXT[row.status] || [];
  const canAssign = ['CREATED', 'ASSIGNED', 'FAILED'].includes(row.status);
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });

  async function run() {
    setBusy(true); setErr('');
    try {
      if (mode === 'assign') await api.post(`/shipments/${row.id}/assign`, { driverId: Number(f.driverId), vehicleId: f.vehicleId ? Number(f.vehicleId) : null });
      else await api.post(`/shipments/${row.id}/status`, { status: f.status, note: f.note || null, podSignedBy: f.podSignedBy || null, failureReason: f.failureReason || null });
      toast('Shipment updated'); setMode(null); setF({}); reload();
    } catch (e) { setErr(errorMessage(e)); } finally { setBusy(false); }
  }

  if (!canAssign && next.length === 0) return null;
  return (
    <>
      {canAssign && <Button variant="ghost" onClick={() => { setF({}); setErr(''); setMode('assign'); }}>Assign</Button>}
      {next.length > 0 && <Button variant="ghost" onClick={() => { setF({ status: next[0] }); setErr(''); setMode('status'); }}>Update status</Button>}
      <Modal open={!!mode} onClose={() => setMode(null)} title={mode === 'assign' ? `Assign ${row.trackingId}` : `Update ${row.trackingId}`}
        footer={<><Button variant="ghost" onClick={() => setMode(null)}>Cancel</Button><Button loading={busy} disabled={mode === 'assign' && !f.driverId} onClick={run}>Save</Button></>}>
        <div className="space-y-4">
          {mode === 'assign' ? (<>
            <Field label="Driver *"><Select value={f.driverId || ''} onChange={set('driverId')}><option value="">Select…</option>{drivers.rows?.map((d) => <option key={d.id} value={d.id}>{d.fullName}</option>)}</Select></Field>
            <Field label="Vehicle"><Select value={f.vehicleId || ''} onChange={set('vehicleId')}><option value="">— keep current —</option>{vehicles.rows?.map((v) => <option key={v.id} value={v.id}>{v.registrationNumber}</option>)}</Select></Field>
          </>) : (<>
            <Field label="New status"><Select value={f.status || ''} onChange={set('status')}>{next.map((s) => <option key={s} value={s}>{s.replaceAll('_', ' ')}</option>)}</Select></Field>
            {f.status === 'DELIVERED' && <Field label="Received by (proof of delivery) *" hint="Receiver name. Drivers can also attach a photo from the mobile app."><Input value={f.podSignedBy || ''} onChange={set('podSignedBy')} /></Field>}
            {f.status === 'FAILED' && <Field label="Failure reason *"><Input value={f.failureReason || ''} onChange={set('failureReason')} /></Field>}
            <Field label="Note (shown to the customer)"><Input value={f.note || ''} onChange={set('note')} maxLength={300} /></Field>
          </>)}
          <ErrorNote>{err}</ErrorNote>
        </div>
      </Modal>
    </>
  );
}
