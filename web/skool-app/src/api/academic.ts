import { api } from './client';
import type { AcademicYear, Subject, Turma } from './types';

export function listAcademicYears(): Promise<AcademicYear[]> {
  return api('/api/academic-years');
}

export function listSubjects(): Promise<Subject[]> {
  return api('/api/subjects');
}

export function listTurmas(academicYearId: string): Promise<Turma[]> {
  return api(`/api/turmas?academicYearId=${academicYearId}`);
}
