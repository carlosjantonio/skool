import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import * as forum from '../api/forum';
import { useAuth } from '../auth/AuthContext';
import type { ForumPost, ForumThread } from '../api/types';

export function ForumThreadPage() {
  const { t } = useTranslation();
  const { threadId } = useParams<{ threadId: string }>();
  const { session } = useAuth();
  const [thread, setThread] = useState<ForumThread | null>(null);
  const [posts, setPosts] = useState<ForumPost[]>([]);
  const [reply, setReply] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [posting, setPosting] = useState(false);

  const canModerate = session?.roles.some((r) => r === 'TEACHER' || r === 'DIRECTOR' || r === 'ADMIN') ?? false;

  useEffect(() => {
    async function load() {
      if (!threadId) return;
      try {
        const detail = await forum.getThread(threadId);
        setThread(detail.thread);
        setPosts(detail.posts);
      } catch (err) {
        setError((err as Error).message);
      } finally {
        setLoading(false);
      }
    }
    void load();
  }, [threadId]);

  async function postReply(e: React.FormEvent) {
    e.preventDefault();
    if (!threadId || !reply.trim()) return;
    setPosting(true);
    try {
      const p = await forum.reply(threadId, reply.trim());
      setPosts((prev) => [...prev, p]);
      setReply('');
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setPosting(false);
    }
  }

  async function upvoteThread() {
    if (!threadId) return;
    const updated = await forum.toggleThreadUpvote(threadId);
    setThread(updated);
  }

  async function upvotePost(postId: string) {
    const updated = await forum.togglePostUpvote(postId);
    setPosts((prev) => prev.map((p) => (p.id === postId ? updated : p)));
  }

  async function pin() {
    if (!thread) return;
    const updated = await forum.moderateThread(thread.id, { pinned: !thread.pinned });
    setThread(updated);
  }

  async function markVerified(postId: string, current: boolean) {
    const updated = await forum.moderatePost(postId, { markedVerified: !current });
    setPosts((prev) => prev.map((p) => (p.id === postId ? updated : p)));
  }

  if (loading) return <div>…</div>;
  if (error) return <div className="error">{error}</div>;
  if (!thread) return null;

  return (
    <div>
      <div className="card">
        <div style={{ display: 'flex', justifyContent: 'space-between', gap: '1rem' }}>
          <div>
            <h1 style={{ marginTop: 0, fontSize: '1.3rem' }}>
              {thread.pinned ? '📌 ' : ''}
              {thread.title}
            </h1>
            <div style={{ fontSize: '0.85rem', color: 'var(--color-text-muted)', marginBottom: '0.5rem' }}>
              {thread.authorName} · {new Date(thread.createdAt).toLocaleString('pt-AO')}
            </div>
            <p style={{ whiteSpace: 'pre-wrap' }}>{thread.body}</p>
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem', alignItems: 'flex-end' }}>
            <button className="secondary" onClick={() => void upvoteThread()}>▲ {thread.upvoteCount}</button>
            {canModerate && (
              <button className="secondary" onClick={() => void pin()}>
                {thread.pinned ? t('forum.unpin') : t('forum.pin')}
              </button>
            )}
          </div>
        </div>
      </div>

      <h2 style={{ marginTop: '1.5rem', fontSize: '1rem' }}>{t('forum.replies', { count: posts.length })}</h2>
      <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
        {posts.map((p) => (
          <div key={p.id} className="card" style={{ padding: '0.75rem 1rem', borderLeft: p.markedVerified ? '3px solid var(--color-accent)' : undefined }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', gap: '1rem' }}>
              <div style={{ flex: 1 }}>
                <div style={{ fontSize: '0.85rem', color: 'var(--color-text-muted)', marginBottom: '0.25rem' }}>
                  {p.authorName} · {t(`role.${p.authorRole}`, p.authorRole)} · {new Date(p.createdAt).toLocaleString('pt-AO')}
                  {p.markedVerified && ` · ${t('forum.verified')}`}
                </div>
                <div style={{ whiteSpace: 'pre-wrap' }}>{p.body}</div>
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.35rem', alignItems: 'flex-end' }}>
                <button className="secondary" onClick={() => void upvotePost(p.id)}>▲ {p.upvoteCount}</button>
                {canModerate && (
                  <button className="secondary" onClick={() => void markVerified(p.id, p.markedVerified)}>
                    {p.markedVerified ? t('forum.unverify') : t('forum.mark_verified')}
                  </button>
                )}
              </div>
            </div>
          </div>
        ))}
      </div>

      <form onSubmit={postReply} className="card" style={{ marginTop: '1rem' }}>
        <h3 style={{ marginTop: 0, fontSize: '1rem' }}>{t('forum.reply_title')}</h3>
        <textarea
          value={reply}
          onChange={(e) => setReply(e.target.value)}
          rows={3}
          placeholder={t('forum.reply_placeholder')}
          style={{ width: '100%', boxSizing: 'border-box', marginBottom: '0.5rem' }}
        />
        <button type="submit" disabled={posting || !reply.trim()}>
          {posting ? t('forum.posting') : t('forum.reply_submit')}
        </button>
      </form>
    </div>
  );
}
