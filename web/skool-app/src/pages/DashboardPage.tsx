import { useTranslation } from 'react-i18next';
import { useAuth } from '../auth/AuthContext';
import type { Role } from '../api/types';

interface RoleView {
  role: Role;
  titleKey: string;
  actions: string[]; // i18n keys
}

const VIEWS: RoleView[] = [
  {
    role: 'ADMIN',
    titleKey: 'dashboard.admin.title',
    actions: ['dashboard.admin.actions.school', 'dashboard.admin.actions.year', 'dashboard.admin.actions.subjects', 'dashboard.admin.actions.turmas'],
  },
  {
    role: 'TEACHER',
    titleKey: 'dashboard.teacher.title',
    actions: ['dashboard.teacher.actions.attendance', 'dashboard.teacher.actions.grades', 'dashboard.teacher.actions.quizzes'],
  },
  {
    role: 'STUDENT',
    titleKey: 'dashboard.student.title',
    actions: ['dashboard.student.actions.grades', 'dashboard.student.actions.quizzes', 'dashboard.student.actions.forum'],
  },
  {
    role: 'GUARDIAN',
    titleKey: 'dashboard.parent.title',
    actions: ['dashboard.parent.actions.children', 'dashboard.parent.actions.fees', 'dashboard.parent.actions.messages'],
  },
];

export function DashboardPage() {
  const { t } = useTranslation();
  const { session } = useAuth();
  if (!session) return null;

  const applicable = VIEWS.filter((v) => session.roles.includes(v.role));

  return (
    <div>
      <h1 style={{ marginTop: 0 }}>{t('dashboard.welcome', { name: session.fullName })}</h1>
      <p style={{ color: 'var(--color-text-muted)' }}>
        {t('dashboard.tenant', { code: session.tenantId.slice(0, 8) })} · {t('dashboard.roles', { roles: session.roles.join(', ') })}
      </p>

      <div style={{ display: 'grid', gap: '1rem', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', marginTop: '1.5rem' }}>
        {applicable.map((view) => (
          <div key={view.role} className="card">
            <h2 style={{ marginTop: 0, fontSize: '1.1rem' }}>{t(view.titleKey)}</h2>
            <ul style={{ paddingLeft: '1.1rem', margin: 0 }}>
              {view.actions.map((a) => (
                <li key={a} style={{ marginBottom: '0.35rem', color: 'var(--color-text-muted)' }}>{t(a)}</li>
              ))}
            </ul>
          </div>
        ))}
      </div>
    </div>
  );
}
