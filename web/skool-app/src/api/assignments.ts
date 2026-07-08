import { api } from './client';
import type { Assignment, AssignmentSubmission } from './types';

export function listStudentAssignments(turmaId: string): Promise<Assignment[]> {
  return api(`/api/student/assignments?turmaId=${turmaId}`);
}

export function submitAssignment(assignmentId: string, notes: string, documentId?: string): Promise<AssignmentSubmission> {
  return api(`/api/student/assignments/${assignmentId}/submit`, {
    method: 'POST',
    body: JSON.stringify({ notes, documentId: documentId ?? null }),
  });
}

export async function mySubmission(assignmentId: string): Promise<AssignmentSubmission | null> {
  try {
    return await api<AssignmentSubmission>(`/api/student/assignments/${assignmentId}/mine`);
  } catch {
    return null;
  }
}

export function mySubmissions(): Promise<AssignmentSubmission[]> {
  return api('/api/student/assignments/mine');
}
