import { api } from './client';
import type { AttendanceBatchResult, AttendanceEntry, AttendanceRecord } from './types';

export function listAttendance(turmaId: string, date: string): Promise<AttendanceRecord[]> {
  return api(`/api/attendance?turmaId=${turmaId}&date=${date}`);
}

export function submitBatch(entries: AttendanceEntry[]): Promise<AttendanceBatchResult> {
  return api('/api/attendance/batch', {
    method: 'POST',
    body: JSON.stringify({ records: entries }),
  });
}
