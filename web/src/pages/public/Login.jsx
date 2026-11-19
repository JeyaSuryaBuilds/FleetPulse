import { useState } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { FleetPulseLogo } from '../../components/brand';
import { useAuth, homeFor } from '../../auth.jsx';
import { errorMessage } from '../../api.js';
import { Button, ErrorNote, Field, Input } from '../../ui.jsx';

export default function Login() {
  const { login, user } = useAuth();
  const nav = useNavigate();
  const loc = useLocation();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [state, setState] = useState({ busy: false, error: '' });
  if (user) return <Navigate to={homeFor(user)} replace />;

  async function submit(e) {
    e.preventDefault();
    setState({ busy: true, error: '' });
    try { const u = await login(email, password); nav(loc.state?.from || homeFor(u), { replace: true }); }
    catch (err) { setState({ busy: false, error: errorMessage(err) }); }
  }

  return (
    <div className="mx-auto flex min-h-[70vh] max-w-md flex-col justify-center px-4 py-16">
      <div className="mb-8 flex justify-center"><FleetPulseLogo variant="horizontal" height={36} decorative /></div>
      <form onSubmit={submit} className="fp-card fp-card--padded space-y-4">
        <h1 className="text-xl font-semibold">Log in to your workspace</h1>
        <Field label="Email"><Input type="email" required autoComplete="username" value={email} onChange={(e) => setEmail(e.target.value)} /></Field>
        <Field label="Password"><Input type="password" required autoComplete="current-password" value={password} onChange={(e) => setPassword(e.target.value)} /></Field>
        <ErrorNote>{state.error}</ErrorNote>
        <Button type="submit" loading={state.busy} className="w-full">Log in</Button>
        <p className="text-center text-sm text-fp-muted">New to FleetPulse? <Link to="/register" className="text-orange underline">Create a workspace</Link></p>
      </form>
    </div>
  );
}
