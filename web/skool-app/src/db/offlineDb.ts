import Dexie, { type EntityTable } from 'dexie';
import type { AttendanceEntry } from '../api/types';

/**
 * Local IndexedDB store used to survive network outages. Each pending row uses the
 * SAME UUID that the backend uses as the attendance PK, so replaying a queued batch
 * after a partial send is idempotent — the backend reports duplicates as skipped,
 * not errors.
 */
export interface PendingAttendance extends AttendanceEntry {
  queuedAt: number;
}

class SkoolOfflineDb extends Dexie {
  attendance!: EntityTable<PendingAttendance, 'id'>;

  constructor() {
    super('skool-offline');
    this.version(1).stores({
      attendance: 'id, turmaId, studentId, date, queuedAt',
    });
  }
}

export const offlineDb = new SkoolOfflineDb();
