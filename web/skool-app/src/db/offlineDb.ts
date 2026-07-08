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

/**
 * A single answer captured mid-quiz. Client-generated {@code id} is what the backend
 * uses as the answer PK — a retried draft after a network blip updates the same row
 * instead of duplicating it.
 */
export interface PendingQuizAnswer {
  id: string;
  attemptId: string;
  questionId: string;
  response: string; // JSON-stringified payload
  queuedAt: number;
  final: 0 | 1; // Dexie can't index booleans — use 0/1
}

class SkoolOfflineDb extends Dexie {
  attendance!: EntityTable<PendingAttendance, 'id'>;
  quizAnswers!: EntityTable<PendingQuizAnswer, 'id'>;

  constructor() {
    super('skool-offline');
    this.version(1).stores({
      attendance: 'id, turmaId, studentId, date, queuedAt',
    });
    this.version(2).stores({
      attendance: 'id, turmaId, studentId, date, queuedAt',
      quizAnswers: 'id, attemptId, questionId, queuedAt, final',
    });
  }
}

export const offlineDb = new SkoolOfflineDb();
