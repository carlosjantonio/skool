import { api } from './client';
import type { Enrollment, Student } from './types';

export function listStudents(): Promise<Student[]> {
  return api('/api/students');
}

export function listEnrollments(academicYearId: string, turmaId?: string): Promise<Enrollment[]> {
  const params = new URLSearchParams({ academicYearId });
  if (turmaId) params.set('turmaId', turmaId);
  return api(`/api/enrollments?${params.toString()}`);
}
