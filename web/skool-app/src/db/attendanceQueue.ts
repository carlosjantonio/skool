import { v5 as uuidv5 } from 'uuid';
import { submitBatch } from '../api/attendance';
import type { AttendanceEntry, AttendanceStatus } from '../api/types';
import { offlineDb, type PendingAttendance } from './offlineDb';

/**
 * DNS namespace UUID used to derive deterministic v5 UUIDs. Backend uses
 * {@code NAMESPACE_DNS} too, so the client and server converge on the same PK for the
 * same (student, turma, date) tuple — retries after a partial sync are seen as
 * duplicates rather than fresh rows.
 */
const NAMESPACE = '6ba7b810-9dad-11d1-80b4-00c04fd430c8';

export function stableAttendanceId(studentId: string, turmaId: string, date: string): string {
  return uuidv5(`attendance:${studentId}:${turmaId}:${date}`, NAMESPACE);
}

export async function enqueue(entry: {
  turmaId: string;
  studentId: string;
  date: string;
  status: AttendanceStatus;
  notes?: string | null;
}): Promise<PendingAttendance> {
  const id = stableAttendanceId(entry.studentId, entry.turmaId, entry.date);
  const row: PendingAttendance = { id, queuedAt: Date.now(), ...entry };
  await offlineDb.attendance.put(row);
  return row;
}

export async function pending(): Promise<PendingAttendance[]> {
  return offlineDb.attendance.orderBy('queuedAt').toArray();
}

export async function countPending(): Promise<number> {
  return offlineDb.attendance.count();
}

export async function clearIds(ids: string[]): Promise<void> {
  await offlineDb.attendance.bulkDelete(ids);
}

export async function flush(): Promise<{ sent: number; skipped: number; remainingQueue: number }> {
  const rows = await pending();
  if (rows.length === 0) return { sent: 0, skipped: 0, remainingQueue: 0 };

  const entries: AttendanceEntry[] = rows.map(({ id, turmaId, studentId, date, status, notes }) =>
    ({ id, turmaId, studentId, date, status, notes: notes ?? null }));

  const result = await submitBatch(entries);
  // Accepted + duplicates can both be removed from the queue.
  const acknowledged = new Set([...result.accepted, ...result.skippedDuplicates]);
  await clearIds([...acknowledged]);
  return {
    sent: result.accepted.length,
    skipped: result.skippedDuplicates.length,
    remainingQueue: rows.length - acknowledged.size,
  };
}
