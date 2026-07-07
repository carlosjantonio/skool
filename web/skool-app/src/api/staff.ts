import { api } from './client';
import type { StaffAssignment, StaffSummary } from './types';

export function getMe(): Promise<StaffSummary> {
  return api('/api/staff/me');
}

export function getMyAssignments(academicYearId: string): Promise<StaffAssignment[]> {
  return api(`/api/staff/me/assignments?academicYearId=${academicYearId}`);
}
