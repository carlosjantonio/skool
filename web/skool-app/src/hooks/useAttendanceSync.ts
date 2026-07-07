import { useCallback, useEffect, useState } from 'react';
import { useOnline } from './useOnline';
import { countPending, flush } from '../db/attendanceQueue';

interface SyncState {
  queued: number;
  syncing: boolean;
  lastSyncAt: number | null;
  error: string | null;
}

/**
 * Watches the pending attendance queue and flushes it whenever the browser is
 * online. Callers get a hook to trigger an immediate flush (e.g. after a manual
 * "sync now" button click) plus current queue depth and syncing status for the UI.
 */
export function useAttendanceSync(): {
  state: SyncState;
  syncNow: () => Promise<void>;
} {
  const online = useOnline();
  const [state, setState] = useState<SyncState>({ queued: 0, syncing: false, lastSyncAt: null, error: null });

  const refreshCount = useCallback(async () => {
    const queued = await countPending();
    setState((s) => ({ ...s, queued }));
  }, []);

  const syncNow = useCallback(async () => {
    setState((s) => ({ ...s, syncing: true, error: null }));
    try {
      await flush();
      const queued = await countPending();
      setState({ queued, syncing: false, lastSyncAt: Date.now(), error: null });
    } catch (err) {
      const queued = await countPending();
      setState({ queued, syncing: false, lastSyncAt: null, error: (err as Error).message });
    }
  }, []);

  // Kick a sync as soon as we come online, then poll queue depth every few seconds
  // so the badge stays fresh even when other tabs enqueue work.
  useEffect(() => { void refreshCount(); }, [refreshCount]);

  useEffect(() => {
    if (online) void syncNow();
  }, [online, syncNow]);

  useEffect(() => {
    const interval = window.setInterval(refreshCount, 4000);
    return () => window.clearInterval(interval);
  }, [refreshCount]);

  return { state, syncNow };
}
