import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { api, errorMessage } from '../../api.js';
import { Button, ErrorNote, Field, Input, Select, Textarea } from '../../ui.jsx';

export default function Contact() {
  const [params] = useSearchParams();
  const [f, setF] = useState({ kind: params.get('type') === 'demo' ? 'DEMO' : 'CONTACT', name: '', email: '', mobile: '', companyName: '', message: '' });
  const [state, setState] = useState({ busy: false, done: false, error: '' });
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });

  async function submit(e) {
    e.preventDefault();
    setState({ busy: true, done: false, error: '' });
    try { await api.post('/public/contact', { ...f, mobile: f.mobile || null, companyName: f.companyName || null, message: f.message || null }); setState({ busy: false, done: true, error: '' }); }
    catch (err) { setState({ busy: false, done: false, error: errorMessage(err) }); }
  }

  return (
    <section className="mx-auto max-w-2xl px-4 py-16 sm:px-6">
      <h1 className="text-4xl font-semibold tracking-tight">{f.kind === 'DEMO' ? 'Request a demo' : 'Contact us'}</h1>
      <p className="mt-3 text-fp-muted">Tell us a little about your fleet and we will get back to you by email.</p>
      {state.done ? (
        <div className="fp-card fp-card--padded mt-8"><p className="font-medium">Thank you. Your message has been received.</p><p className="mt-1 text-sm text-fp-muted">We will reply to {f.email} soon.</p></div>
      ) : (
        <form onSubmit={submit} className="fp-card fp-card--padded mt-8 space-y-4">
          <Field label="I would like to"><Select value={f.kind} onChange={set('kind')}><option value="DEMO">Request a demo</option><option value="CONTACT">Ask a question</option></Select></Field>
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Your name"><Input required maxLength={160} value={f.name} onChange={set('name')} autoComplete="name" /></Field>
            <Field label="Email"><Input required type="email" maxLength={190} value={f.email} onChange={set('email')} autoComplete="email" /></Field>
            <Field label="Mobile (optional)"><Input value={f.mobile} onChange={set('mobile')} placeholder="+14155550123" autoComplete="tel" /></Field>
            <Field label="Company (optional)"><Input maxLength={160} value={f.companyName} onChange={set('companyName')} autoComplete="organization" /></Field>
          </div>
          <Field label="Message"><Textarea maxLength={4000} value={f.message} onChange={set('message')} /></Field>
          <ErrorNote>{state.error}</ErrorNote>
          <Button type="submit" loading={state.busy}>Send</Button>
        </form>
      )}
    </section>
  );
}
