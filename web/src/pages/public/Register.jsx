import { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';

import { FleetPulseLogo } from '../../components/brand';
import { api, errorMessage } from '../../api.js';
import { useAuth } from '../../auth.jsx';
import { Badge, Button, ErrorNote, Field, Input } from '../../ui.jsx';

export default function Register() {

    const [params] = useSearchParams();
    const nav = useNavigate();

    const { acceptSession } = useAuth();

    const [f, setF] = useState({
        companyName: '',
        companyEmail: '',
        mobile: '',
        adminFullName: '',
        password: '',
        confirmPassword: '',
        planCode: params.get('plan') || ''
    });

    const [busy, setBusy] = useState(false);
    const [error, setError] = useState('');
    const [fieldErrors, setFieldErrors] = useState({});

    const set = (key) => (e) => {
        setF({
            ...f,
            [key]: e.target.value
        });
    };

    async function start(e) {

        e.preventDefault();

        setError('');
        setFieldErrors({});

        if (f.password !== f.confirmPassword) {
            setFieldErrors({
                confirmPassword: 'Passwords do not match'
            });
            return;
        }

        setBusy(true);

        try {

            const { data } = await api.post(
                '/registration/start',
                {
                    ...f,
                    planCode: f.planCode || null
                }
            );

            if (data.complete && data.session) {

                acceptSession(data.session);

                nav('/app', {
                    replace: true
                });

                return;
            }

            setError(
                'Registration started. OTP verification is required.'
            );

        } catch (err) {

            setError(errorMessage(err));

            setFieldErrors(
                err?.response?.data?.fields || {}
            );

        } finally {

            setBusy(false);
        }
    }

    return (

        <div className="mx-auto max-w-xl px-4 py-14">

            <div className="mb-6 flex justify-center">
                <FleetPulseLogo
                    variant="horizontal"
                    height={36}
                    decorative
                />
            </div>

            <div className="fp-card fp-card--padded">

                <div className="mb-5">

                    <h1 className="text-xl font-semibold">
                        Create your workspace
                    </h1>

                    <p className="mt-1 text-sm text-fp-muted">
                        Register your company and create your admin account.
                    </p>

                </div>

                <form
                    onSubmit={start}
                    className="space-y-4"
                >

                    <Field
                        label="Company name"
                        error={fieldErrors.companyName}
                    >
                        <Input
                            required
                            minLength={2}
                            maxLength={160}
                            value={f.companyName}
                            onChange={set('companyName')}
                            autoComplete="organization"
                        />
                    </Field>

                    <div className="grid gap-4 sm:grid-cols-2">

                        <Field
                            label="Company email"
                            error={fieldErrors.companyEmail}
                        >
                            <Input
                                type="email"
                                required
                                value={f.companyEmail}
                                onChange={set('companyEmail')}
                                autoComplete="email"
                            />
                        </Field>

                        <Field
                            label="Mobile number"
                            error={fieldErrors.mobile}
                        >
                            <Input
                                required
                                value={f.mobile}
                                onChange={set('mobile')}
                                placeholder="+14155550123"
                                autoComplete="tel"
                            />
                        </Field>

                    </div>

                    <Field
                        label="Admin full name"
                        error={fieldErrors.adminFullName}
                    >
                        <Input
                            required
                            value={f.adminFullName}
                            onChange={set('adminFullName')}
                            autoComplete="name"
                        />
                    </Field>

                    <div className="grid gap-4 sm:grid-cols-2">

                        <Field
                            label="Password"
                            error={fieldErrors.password}
                        >
                            <Input
                                type="password"
                                required
                                minLength={10}
                                value={f.password}
                                onChange={set('password')}
                                autoComplete="new-password"
                            />
                        </Field>

                        <Field
                            label="Confirm password"
                            error={fieldErrors.confirmPassword}
                        >
                            <Input
                                type="password"
                                required
                                value={f.confirmPassword}
                                onChange={set('confirmPassword')}
                                autoComplete="new-password"
                            />
                        </Field>

                    </div>

                    {f.planCode && (

                        <p className="text-sm text-fp-muted">
                            Selected plan: <Badge>{f.planCode}</Badge>
                        </p>

                    )}

                    <ErrorNote>
                        {error}
                    </ErrorNote>

                    <Button
                        type="submit"
                        loading={busy}
                        className="w-full"
                    >
                        Create Account
                    </Button>

                    <p className="text-center text-sm text-fp-muted">

                        Already registered?{' '}

                        <Link
                            to="/login"
                            className="text-orange underline"
                        >
                            Log in
                        </Link>

                    </p>

                </form>

            </div>

        </div>
    );
}