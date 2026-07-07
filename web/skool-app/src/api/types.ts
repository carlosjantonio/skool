export type Role = 'ADMIN' | 'DIRECTOR' | 'SECRETARY' | 'TEACHER' | 'STUDENT' | 'GUARDIAN' | 'MINISTRY';

export interface TokenResponse {
  accessToken: string;
  accessTokenExpiresAt: string;
  refreshToken: string;
  refreshTokenExpiresAt: string;
  userId: string;
  tenantId: string;
  email: string;
  fullName: string;
  roles: Role[];
}

export interface ProblemDetail {
  type: string;
  title: string;
  status: number;
  detail: string;
  instance?: string;
  timestamp?: string;
  errors?: { field: string; message: string }[];
}

export interface School {
  id: string;
  name: string;
  code: string;
  municipioId: string;
  comunaOuBairro: string | null;
  addressLine1: string | null;
  addressComplement: string | null;
  active: boolean;
}

export interface Trimester {
  id: string;
  key: 'T1' | 'T2' | 'T3';
  startDate: string;
  endDate: string;
}

export interface AcademicYear {
  id: string;
  name: string;
  startDate: string;
  endDate: string;
  current: boolean;
  trimesters: Trimester[];
}

export interface Subject {
  id: string;
  name: string;
  code: string;
  gradeLevel: string;
}

export interface Turma {
  id: string;
  schoolId: string;
  academicYearId: string;
  name: string;
  gradeLevel: string;
  track: string;
  capacity: number;
}
