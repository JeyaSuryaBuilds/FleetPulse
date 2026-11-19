import { useCallback, useEffect, useState } from 'react';
import { Plus } from 'lucide-react';
import { api, errorMessage } from '../../api.js';
import { useAuth } from '../../auth.jsx';
import { Button, ErrorNote, Field, Input, Modal, PageHeader, Select, Spinner, StatusBadge, fmtDateTime, useToast } from '../../ui.jsx';

export default function Users() {
  const { user } = useAuth();
  const toast = useToast();
  const [rows, setRows] = useState(null);
  const [roles, setRoles] = useState([]);
  const [error, setError] = useState('');
  const [create, setCreate] = useState(null);
  const [edit, setEdit] = useState(null);
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState('');
  const load = useCallback(() => api.get('/users').then((r) => setRows(r.data)).catch((e) => setError(errorMessage(e))), []);
  useEffect(() => { load(); api.get('/users/roles').then((r) => setRoles(r.data)).catch(() => {}); }, [load]);

  async function submitCreate(e) {
    e.preventDefault(); setBusy(true); setErr('');
    try { await api.post('/users', create); toast('User created'); setCreate(null); load(); } catch (x) { setErr(errorMessage(x)); } finally { setBusy(false); }
  }
  async function submitEdit(e) {
    e.preventDefault(); setBusy(true); setErr('');
    const body = { role: edit.role, status: edit.status, newPassword: edit.newPassword || null };
    if (edit.id === user.id) { delete body.role; delete body.status; }
    try { await api.patch(`/users/${edit.id}`, body); toast('User updated'); setEdit(null); load(); } catch (x) { setErr(errorMessage(x)); } finally { setBusy(false); }
  }

  return (
    <>
      <PageHeader title="Users and roles" subtitle="Roles are enforced by the server, not only by the menu." actions={<Button onClick={() => { setErr(''); setCreate({ fullName: '', email: '', mobile: '', password: '', role: 'OPERATIONS_MANAGER' }); }}><Plus size={16} aria-hidden /> New user</Button>} />
      <ErrorNote>{error}</ErrorNote>
      <div className="fp-card overflow-x-auto">{!rows ? <Spinner /> : (
        <table className="fp-table"><thead><tr><th>ID</th><th>Name</th><th>Email</th><th>Role</th><th>Status</th><th>Last login</th><th /></tr></thead>
          <tbody>{rows.map((u) => <tr key={u.id}><td>{u.id}</td><td>{u.fullName}</td><td>{u.email}</td><td>{u.role.replaceAll('_', ' ')}</td><td><StatusBadge value={u.status} /></td><td>{fmtDateTime(u.lastLoginAt)}</td>
            <td className="text-right"><Button variant="ghost" onClick={() => { setErr(''); setEdit({ id: u.id, role: u.role, status: u.status, newPassword: '' }); }}>Edit</Button></td></tr>)}</tbody></table>)}</div>
      <p className="mt-3 text-xs text-fp-muted">To let a driver or employee use the mobile app, create a user with the Driver (or another) role, then enter the user ID in their Driver or Employee record.</p>

      <Modal open={!!create} onClose={() => setCreate(null)} title="New user" footer={<><Button variant="ghost" onClick={() => setCreate(null)}>Cancel</Button><Button type="submit" form="uc" loading={busy}>Create</Button></>}>
        {create && <form id="uc" onSubmit={submitCreate} className="space-y-4">
          <Field label="Full name"><Input required value={create.fullName} onChange={(e) => setCreate({ ...create, fullName: e.target.value })} /></Field>
          <Field label="Email"><Input type="email" required value={create.email} onChange={(e) => setCreate({ ...create, email: e.target.value })} /></Field>
          <Field label="Mobile (optional)"><Input value={create.mobile} onChange={(e) => setCreate({ ...create, mobile: e.target.value })} /></Field>
          <Field label="Role"><Select value={create.role} onChange={(e) => setCreate({ ...create, role: e.target.value })}>{roles.map((r) => <option key={r} value={r}>{r.replaceAll('_', ' ')}</option>)}</Select></Field>
          <Field label="Temporary password" hint="At least 10 characters. Share it securely."><Input type="password" required minLength={10} autoComplete="new-password" value={create.password} onChange={(e) => setCreate({ ...create, password: e.target.value })} /></Field>
          <ErrorNote>{err}</ErrorNote></form>}
      </Modal>
      <Modal open={!!edit} onClose={() => setEdit(null)} title="Edit user" footer={<><Button variant="ghost" onClick={() => setEdit(null)}>Cancel</Button><Button type="submit" form="ue" loading={busy}>Save</Button></>}>
        {edit && <form id="ue" onSubmit={submitEdit} className="space-y-4">
          {edit.id !== user.id && <><Field label="Role"><Select value={edit.role} onChange={(e) => setEdit({ ...edit, role: e.target.value })}>{roles.map((r) => <option key={r} value={r}>{r.replaceAll('_', ' ')}</option>)}</Select></Field>
            <Field label="Status"><Select value={edit.status} onChange={(e) => setEdit({ ...edit, status: e.target.value })}><option value="ACTIVE">Active</option><option value="DISABLED">Disabled</option></Select></Field></>}
          <Field label="New password (optional)" hint="Setting a password signs the user out everywhere."><Input type="password" minLength={10} autoComplete="new-password" value={edit.newPassword} onChange={(e) => setEdit({ ...edit, newPassword: e.target.value })} /></Field>
          <ErrorNote>{err}</ErrorNote></form>}
      </Modal>
    </>
  );
}
