import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { ApiError } from '../api/client';

export function LoginPage() {
  const { t } = useTranslation();
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const from = (location.state as { from?: { pathname: string } } | undefined)?.from?.pathname || '/';

  const [email, setEmail] = useState('admin@skool.demo');
  const [password, setPassword] = useState('admin123');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await login(email, password);
      navigate(from, { replace: true });
    } catch (err) {
      const msg = err instanceof ApiError && err.problem?.detail ? err.problem.detail : t('login.error');
      setError(msg);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="center-screen">
      <div className="card" style={{ width: '100%', maxWidth: 420 }}>
        <h1 style={{ marginTop: 0, marginBottom: 0.25 + 'rem' }}>{t('app.name')}</h1>
        <p style={{ color: 'var(--color-text-muted)', marginTop: 0 }}>{t('app.tagline')}</p>

        <h2 style={{ marginBottom: '1rem', fontSize: '1.1rem' }}>{t('login.title')}</h2>

        {error && <div className="error">{error}</div>}

        <form onSubmit={onSubmit}>
          <div className="form-field">
            <label htmlFor="email">{t('login.email')}</label>
            <input id="email" type="email" required value={email} onChange={(e) => setEmail(e.target.value)} autoComplete="email" />
          </div>
          <div className="form-field">
            <label htmlFor="password">{t('login.password')}</label>
            <input id="password" type="password" required value={password} onChange={(e) => setPassword(e.target.value)} autoComplete="current-password" />
          </div>
          <button type="submit" disabled={submitting} style={{ width: '100%' }}>
            {submitting ? t('login.submitting') : t('login.submit')}
          </button>
        </form>
      </div>
    </div>
  );
}
