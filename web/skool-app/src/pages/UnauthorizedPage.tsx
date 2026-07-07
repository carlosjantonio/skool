import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';

export function UnauthorizedPage() {
  const { t } = useTranslation();
  return (
    <div className="center-screen">
      <div className="card" style={{ maxWidth: 420, textAlign: 'center' }}>
        <h1 style={{ marginTop: 0 }}>{t('unauthorized.title')}</h1>
        <p>{t('unauthorized.detail')}</p>
        <Link to="/">{t('unauthorized.back')}</Link>
      </div>
    </div>
  );
}
