import { useTranslation } from 'react-i18next';
import { useOnline } from '../hooks/useOnline';
import { useAttendanceSync } from '../hooks/useAttendanceSync';

/**
 * Renders a small chip in the header showing whether the browser is online and how
 * many attendance mutations are still queued locally. Clicking triggers an
 * immediate flush attempt.
 */
export function OfflineIndicator() {
  const { t } = useTranslation();
  const online = useOnline();
  const { state, syncNow } = useAttendanceSync();
  const queued = state.queued;

  const label = !online
    ? t('offline.offline')
    : queued > 0
      ? state.syncing
        ? t('offline.syncing')
        : t('offline.queued', { count: queued })
      : t('offline.online');

  const background = !online
    ? 'rgba(179, 38, 30, 0.85)'
    : queued > 0
      ? 'rgba(240, 180, 41, 0.9)'
      : 'rgba(255, 255, 255, 0.15)';

  return (
    <button
      onClick={() => void syncNow()}
      disabled={!online || state.syncing || queued === 0}
      title={state.error ?? undefined}
      style={{
        background,
        color: '#fff',
        border: '1px solid rgba(255,255,255,0.4)',
        borderRadius: 999,
        padding: '0.25rem 0.75rem',
        fontSize: '0.8rem',
        cursor: queued > 0 && online ? 'pointer' : 'default',
      }}
    >
      {label}
    </button>
  );
}
