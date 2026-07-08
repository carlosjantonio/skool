import { api } from './client';
import type { Forum, ForumPost, ForumThread } from './types';

export function listForums(turmaId: string): Promise<Forum[]> {
  return api(`/api/forums?turmaId=${turmaId}`);
}

export function listThreads(forumId: string): Promise<ForumThread[]> {
  return api(`/api/forums/${forumId}/threads`);
}

export interface ThreadDetail {
  thread: ForumThread;
  posts: ForumPost[];
}

export function getThread(threadId: string): Promise<ThreadDetail> {
  return api(`/api/forums/threads/${threadId}`);
}

export function createThread(forumId: string, title: string, body: string): Promise<ForumThread> {
  return api(`/api/forums/${forumId}/threads`, {
    method: 'POST',
    body: JSON.stringify({ title, body }),
  });
}

export function reply(threadId: string, body: string): Promise<ForumPost> {
  return api(`/api/forums/threads/${threadId}/posts`, {
    method: 'POST',
    body: JSON.stringify({ body }),
  });
}

export function toggleThreadUpvote(threadId: string): Promise<ForumThread> {
  return api(`/api/forums/threads/${threadId}/upvote`, { method: 'POST' });
}

export function togglePostUpvote(postId: string): Promise<ForumPost> {
  return api(`/api/forums/posts/${postId}/upvote`, { method: 'POST' });
}

export function moderateThread(threadId: string, flags: { pinned?: boolean; hidden?: boolean }): Promise<ForumThread> {
  return api(`/api/forums/threads/${threadId}/moderation`, {
    method: 'PATCH',
    body: JSON.stringify(flags),
  });
}

export function moderatePost(postId: string, flags: { hidden?: boolean; markedVerified?: boolean }): Promise<ForumPost> {
  return api(`/api/forums/posts/${postId}/moderation`, {
    method: 'PATCH',
    body: JSON.stringify(flags),
  });
}
