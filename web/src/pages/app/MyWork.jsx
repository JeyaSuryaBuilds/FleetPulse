import { useEffect, useState } from 'react';
import { api, errorMessage } from '../../api.js';
import { EmptyState, ErrorNote, PageHeader, Spinner, Stat, StatusBadge, fmtDate, fmtDateTime, fmtMoney } from '../../ui.jsx';

/** Own records only (the /me endpoints resolve the caller on the server). GPS, camera and proof of delivery capture happen in the mobile apps. */
export default function MyWork({ standalone = false }) {
  const [s, setS] = useState({ loading: true });
  useEffect(() => {
    const get = (p) => api.get(p).then((r) => r.data).catch((e) => ({ __error: errorMessage(e), __status: e?.response?.status }));
    Promise.all([get('/me/profile'), get('/me/payslips'), get('/me/earnings'), get('/me/attendance')]).then(([profile, payslips, earnings, attendance]) => setS({ loading: false, profile, payslips, earnings, attendance }));
  }, []);
  if (s.loading) return <Spinner />;
  const ok = (x) => x && !x.__error;
  return (
    <>
      {standalone && <PageHeader title="My work and pay" subtitle="Only your own records are shown here" />}
      {ok(s.profile) && <div className="fp-card fp-card--padded mb-4"><p className="font-medium">{s.profile.fullName}</p><p className="text-sm text-fp-muted">{s.profile.company}{s.profile.assignedVehicle ? ` · Vehicle ${s.profile.assignedVehicle.registrationNumber}` : ''}</p>
        <p className="mt-2 text-sm text-fp-muted">Live GPS sharing, delivery updates, proof of delivery photos and the barcode scanner are available in the FleetPulse mobile app.</p></div>}
      <div className="grid gap-4 sm:grid-cols-3">
        {ok(s.earnings) ? (<><Stat label="Awaiting approval" value={fmtMoney(s.earnings.pendingApproval)} /><Stat label="Approved, unpaid" value={fmtMoney(s.earnings.approvedUnpaid)} /><Stat label="Paid total" value={fmtMoney(s.earnings.paidTotal)} /></>) : <p className="text-sm text-fp-muted sm:col-span-3">{s.earnings?.__status === 403 ? 'No employee profile is linked to your user, so there are no earnings to show.' : s.earnings?.__error}</p>}
      </div>
      <h2 className="mb-2 mt-8 font-medium">Payslips</h2>
      <div className="fp-card overflow-x-auto">{!ok(s.payslips) || s.payslips.length === 0 ? <EmptyState title="No payslips yet" /> : (
        <table className="fp-table"><thead><tr><th>Period</th><th>Model</th><th>Basic</th><th>Work earnings</th><th>Deductions</th><th>Net</th><th>Status</th></tr></thead>
          <tbody>{s.payslips.map((p) => <tr key={p.id}><td>{p.periodMonth}</td><td>{p.model}</td><td>{fmtMoney(p.basic)}</td><td>{fmtMoney(p.workEarnings)}</td><td>{fmtMoney(Number(p.deductions) + Number(p.advances))}</td><td>{fmtMoney(p.netSalary)}</td><td><StatusBadge value={p.status} /></td></tr>)}</tbody></table>)}</div>
      <h2 className="mb-2 mt-8 font-medium">Recent attendance</h2>
      <div className="fp-card overflow-x-auto">{!ok(s.attendance) || s.attendance.length === 0 ? <EmptyState title="No attendance records" /> : (
        <table className="fp-table"><thead><tr><th>Date</th><th>In</th><th>Out</th><th>Status</th></tr></thead><tbody>{s.attendance.slice(0, 20).map((a, i) => <tr key={i}><td>{fmtDate(a.workDate)}</td><td>{fmtDateTime(a.checkIn)}</td><td>{fmtDateTime(a.checkOut)}</td><td><StatusBadge value={a.status} /></td></tr>)}</tbody></table>)}</div>
    </>
  );
}
