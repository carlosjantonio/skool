import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import * as forum from '../api/forum';
import type { ForumThread } from '../api/types';

export function ForumPage() {
  const { t } = useTranslation();
  const { forumId } = useParams<{ forumId: string }>();
  const [threads, setThreads] = useState<ForumThread[]>([]);
  const [title, setTitle] = useState('');
  const [body, setBody] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [posting, setPosting] = useState(false);

  useEffect(() => {
    async function load() {
      if (!forumId) return;
      try {
        setThreads(await forum.listThreads(forumId));
      } catch (err) {
        setError((err as Error).message);
      } finally {
        setLoading(false);
      }
    }
    void load();
  }, [forumId]);

  async function createThread(e: React.FormEvent) {
    e.preventDefault();
    if (!forumId || !title.trim() || !body.trim()) return;
    setPosting(true);
    try {
      const t = await forum.createThread(forumId, title.trim(), body.trim());
      setThreads((prev) => [t, ...prev]);
      setTitle('');
      setBody('');
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setPosting(false);
    }
  }

  if (loading) return <div>…</div>;
  if (error) return <div className="error">{error}</div>;

  return (
    <div>
      <h1 style={{ marginTop: 0 }}>{t('forum.threads')}</h1>

      <form onSubmit={createThread} className="card" style={{ marginBottom: '1rem' }}>
        <h2 style={{ marginTop: 0, fontSize: '1rem' }}>{t('forum.new_thread')}</h2>
        <input
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          placeholder={t('forum.title_placeholder')}
          style={{ width: '100%', boxSizing: 'border-box', marginBottom: '0.5rem' }}
        />
        <textarea
          value={body}
          onChange={(e) => setBody(e.target.value)}
          placeholder={t('forum.body_placeholder')}
          rows={3}
          style={{ width: '100%', boxSizing: 'border-box', marginBottom: '0.5rem' }}
        />
        <button type="submit" disabled={posting || !title.trim() || !body.trim()}>
          {posting ? t('forum.posting') : t('forum.post')}
        </button>
      </form>

      {threads.length === 0 ? (
        <div className="card" style={{ color: 'var(--color-text-muted)' }}>{t('forum.empty')}</div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
          {threads.map((t) => (
            <Link key={t.id} to={`/student/forums/threads/${t.id}`}>
              <div className="card" style={{ padding: '0.75rem 1rem' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', gap: '0.75rem' }}>
                  <div>
                    <div style={{ fontWeight: 600 }}>
                      {t.pinned ? '📌 ' : ''}
                      {t.title}
                    </div>
                    <div style={{ fontSize: '0.85rem', color: 'var(--color-text-muted)', marginTop: '0.25rem' }}>
                      {t.authorName} · {new Date(t.lastActivityAt).toLocaleString('pt-AO')}
                    </div>
                  </div>
                  <div style={{ fontSize: '0.85rem', color: 'var(--color-text-muted)', textAlign: 'right', flexShrink: 0 }}>
                    <div>▲ {t.upvoteCount}</div>
                    <div>{t.replyCount} 💬</div>
                  </div>
                </div>
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
