import { api } from './client';
import type { StudentGradeSummary, StudentSelf } from './types';

export function getMe(): Promise<StudentSelf> {
  return api('/api/student/me');
}

export function getMyGradeSummary(studentId: string, academicYearId: string): Promise<StudentGradeSummary> {
  return api(`/api/grades/students/${studentId}/summary?academicYearId=${academicYearId}`);
}
