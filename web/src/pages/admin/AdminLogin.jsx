import { useState } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { Lock } from 'lucide-react';
import { FleetPulseLogo } from '../../components/brand';
import { api, adminSession, errorMessage } from '../../api.js';
import { Button, ErrorNote, Field, Input } from '../../ui.jsx';

/** Super Admin sign-in: a single password field. No username, email, mobile number or OTP. */
export default function AdminLogin() {
  const nav = useNavigate();
  const [password, setPassword] = useState('');
  const [state, setState] = useState({ busy: false, error: '' });
  if (adminSession.get()?.accessToken) return <Navigate to="/admin/dashboard" replace />;

  async function submit(e) {
    e.preventDefault();
    setState({ busy: true, error: '' });
    try { const { data } = await api.post('/admin/auth/login', { password }); adminSession.set(data); nav('/admin/dashboard', { replace: true }); }
    catch (err) { setState({ busy: false, error: errorMessage(err) }); setPassword(''); }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-fp-bg px-4">
      <form onSubmit={submit} className="fp-card fp-card--padded w-full max-w-sm space-y-4">
        <div className="flex justify-center"><FleetPulseLogo variant="horizontal" height={32} decorative /></div>
        <p className="flex items-center justify-center gap-2 text-sm text-fp-muted"><Lock size={14} aria-hidden /> Fayzo Technologies Super Admin</p>
        <Field label="Password"><Input type="password" required autoFocus autoComplete="current-password" value={password} onChange={(e) => setPassword(e.target.value)} /></Field>
        <ErrorNote>{state.error}</ErrorNote>
        <Button type="submit" loading={state.busy} className="w-full">Sign in</Button>
      </form>
    </div>
  );
}
