import { api } from './client';
import type { GradeEntry, GradeResponse } from './types';

export function submitGrades(grades: GradeEntry[]): Promise<GradeResponse[]> {
  return api('/api/grades/batch', {
    method: 'POST',
    body: JSON.stringify({ grades }),
  });
}
