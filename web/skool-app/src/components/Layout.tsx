import { useTranslation } from 'react-i18next';
import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { OfflineIndicator } from './OfflineIndicator';

export function Layout() {
  const { t } = useTranslation();
  const { session, logout, hasRole } = useAuth();
  const navigate = useNavigate();

  async function handleLogout() {
    await logout();
    navigate('/login', { replace: true });
  }

  const teacherLike = hasRole('TEACHER') || hasRole('DIRECTOR') || hasRole('ADMIN');

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      <header style={{ background: 'var(--color-primary)', color: '#fff', padding: '0.75rem 1.5rem', display: 'flex', alignItems: 'center', gap: '1rem', flexWrap: 'wrap' }}>
        <Link to="/" style={{ color: '#fff', textDecoration: 'none', fontWeight: 600 }}>{t('app.name')}</Link>
        <span style={{ fontSize: '0.85rem', opacity: 0.85 }}>{t('app.tagline')}</span>

        <nav style={{ display: 'flex', gap: '0.75rem', marginLeft: '1rem' }}>
          <NavLink to="/" end style={({ isActive }) => ({ color: '#fff', textDecoration: 'none', opacity: isActive ? 1 : 0.75 })}>
            {t('nav.dashboard')}
          </NavLink>
          {teacherLike && (
            <NavLink to="/classes" style={({ isActive }) => ({ color: '#fff', textDecoration: 'none', opacity: isActive ? 1 : 0.75 })}>
              {t('nav.classes')}
            </NavLink>
          )}
        </nav>

        <div style={{ marginLeft: 'auto', display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <OfflineIndicator />
          {session && <span style={{ fontSize: '0.9rem' }}>{session.email}</span>}
          <button className="secondary" onClick={handleLogout} style={{ background: 'transparent', color: '#fff', borderColor: '#fff' }}>
            {t('nav.logout')}
          </button>
        </div>
      </header>
      <main style={{ flex: 1, padding: '1.5rem', maxWidth: 1100, margin: '0 auto', width: '100%' }}>
        <Outlet />
      </main>
    </div>
  );
}
