import client from './client';
import type { Session, Message, ChatResponse, SendMessagePayload } from '../types';

export const agentApi = {
  // ── Sessions ──────────────────────────────────────────

  getSessions: async (): Promise<Session[]> => {
    const { data } = await client.get<Session[]>('/sessions');
    return data;
  },

  createSession: async (userId?: string): Promise<Session> => {
    const params = userId ? { userId } : {};
    const { data } = await client.post<Session>('/sessions', null, { params });
    return data;
  },

  deleteSession: async (sessionId: string): Promise<void> => {
    await client.delete(`/sessions/${sessionId}`);
  },

  // ── Messages ──────────────────────────────────────────

  getMessages: async (sessionId: string): Promise<Message[]> => {
    const { data } = await client.get<Message[]>(`/sessions/${sessionId}/messages`);
    return data;
  },

  // ── Chat ──────────────────────────────────────────────

  /** Новая сессия + первое сообщение */
  startChat: async (payload: SendMessagePayload): Promise<ChatResponse> => {
    const { data } = await client.post<ChatResponse>('/chat', payload);
    return data;
  },

  /** Сообщение в существующую сессию */
  sendMessage: async (sessionId: string, message: string): Promise<ChatResponse> => {
    const { data } = await client.post<ChatResponse>(`/sessions/${sessionId}/chat`, { message });
    return data;
  },

  // ── Tools ─────────────────────────────────────────────

  getTools: async (): Promise<Record<string, string>> => {
    const { data } = await client.get<Record<string, string>>('/tools');
    return data;
  },
};
