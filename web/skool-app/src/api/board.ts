import { api } from './client';
import type { BoardEntry } from './types';

export function feed(turmaId: string, subjectId?: string): Promise<BoardEntry[]> {
  const q = subjectId ? `?turmaId=${turmaId}&subjectId=${subjectId}` : `?turmaId=${turmaId}`;
  return api(`/api/board${q}`);
}
