import { create } from 'zustand';
import { agentApi } from '../api/agentApi';
import type { Session, Message } from '../types';

interface ChatStore {
  // State
  sessions: Session[];
  activeSessionId: string | null;
  messages: Record<string, Message[]>; // sessionId → messages
  isLoading: boolean;
  isSending: boolean;
  error: string | null;

  // Actions
  loadSessions: () => Promise<void>;
  selectSession: (sessionId: string) => Promise<void>;
  createNewSession: () => Promise<string>;
  deleteSession: (sessionId: string) => Promise<void>;
  sendMessage: (text: string) => Promise<void>;
  clearError: () => void;

  // Optimistic update helpers (internal)
  _addOptimisticMessage: (sessionId: string, content: string) => void;
  _addAssistantMessage: (sessionId: string, content: string) => void;
  _removeLastMessage: (sessionId: string) => void;
}

let optimisticIdCounter = -1;

export const useChatStore = create<ChatStore>((set, get) => ({
  sessions: [],
  activeSessionId: null,
  messages: {},
  isLoading: false,
  isSending: false,
  error: null,

  clearError: () => set({ error: null }),

  loadSessions: async () => {
    set({ isLoading: true, error: null });
    try {
      const sessions = await agentApi.getSessions();
      set({ sessions, isLoading: false });
    } catch (e) {
      set({ error: 'Failed to load sessions', isLoading: false });
    }
  },

  selectSession: async (sessionId) => {
    set({ activeSessionId: sessionId, error: null });
    // Загружаем историю если ещё не загружена
    if (!get().messages[sessionId]) {
      set({ isLoading: true });
      try {
        const msgs = await agentApi.getMessages(sessionId);
        set(state => ({
          messages: { ...state.messages, [sessionId]: msgs },
          isLoading: false,
        }));
      } catch {
        set({ isLoading: false });
      }
    }
  },

  createNewSession: async () => {
    const session = await agentApi.createSession('user1');
    set(state => ({
      sessions: [session, ...state.sessions],
      activeSessionId: session.id,
      messages: { ...state.messages, [session.id]: [] },
    }));
    return session.id;
  },

  deleteSession: async (sessionId) => {
    await agentApi.deleteSession(sessionId);
    set(state => {
      const newMessages = { ...state.messages };
      delete newMessages[sessionId];
      const newSessions = state.sessions.filter(s => s.id !== sessionId);
      return {
        sessions: newSessions,
        messages: newMessages,
        activeSessionId: state.activeSessionId === sessionId
          ? (newSessions[0]?.id ?? null)
          : state.activeSessionId,
      };
    });
  },

  sendMessage: async (text) => {
    if (!text.trim() || get().isSending) return;

    const { activeSessionId } = get();
    set({ isSending: true, error: null });

    let sessionId = activeSessionId;

    // Оптимистично добавляем сообщение пользователя
    const userMsg: Message = {
      id: optimisticIdCounter--,
      role: 'USER',
      content: text,
      toolName: null,
      createdAt: new Date().toISOString(),
    };

    if (sessionId) {
      set(state => ({
        messages: {
          ...state.messages,
          [sessionId!]: [...(state.messages[sessionId!] ?? []), userMsg],
        },
      }));
    }

    // Добавляем "печатает..." индикатор
    const typingMsg: Message = {
      id: optimisticIdCounter--,
      role: 'ASSISTANT',
      content: '__typing__',
      toolName: null,
      createdAt: new Date().toISOString(),
    };

    const addTyping = (sid: string) => {
      set(state => ({
        messages: {
          ...state.messages,
          [sid]: [...(state.messages[sid] ?? []), typingMsg],
        },
      }));
    };

    const removeTyping = (sid: string) => {
      set(state => ({
        messages: {
          ...state.messages,
          [sid]: (state.messages[sid] ?? []).filter(m => m.content !== '__typing__'),
        },
      }));
    };

    try {
      let responseText: string;
      let newSessionCreated = false;

      if (!sessionId) {
        // Нет активной сессии — создаём новую через /api/agent/chat
        const { data } = await import('axios').then(m =>
          m.default.post('http://localhost:8081/api/agent/chat',
            { message: text, userId: 'user1' },
            { headers: { 'Content-Type': 'application/json' }, timeout: 120_000 }
          )
        );
        sessionId = data.sessionId;
        responseText = data.message;
        newSessionCreated = true;

        // Обновляем optimistic messages на реальный sessionId
        set(state => ({
          messages: {
            ...state.messages,
            [sessionId!]: [userMsg],
          },
          activeSessionId: sessionId,
        }));
      } else {
        addTyping(sessionId);
        const resp = await agentApi.sendMessage(sessionId, text);
        responseText = resp.message;
        removeTyping(sessionId);
      }

      const assistantMsg: Message = {
        id: optimisticIdCounter--,
        role: 'ASSISTANT',
        content: responseText,
        toolName: null,
        createdAt: new Date().toISOString(),
      };

      set(state => ({
        messages: {
          ...state.messages,
          [sessionId!]: [...(state.messages[sessionId!] ?? []), assistantMsg],
        },
        isSending: false,
      }));

      // Обновляем список сессий
      await get().loadSessions();
      if (newSessionCreated) {
        set({ activeSessionId: sessionId });
      }

    } catch (e: unknown) {
      const sid = sessionId ?? activeSessionId ?? '';
      if (sid) removeTyping(sid);
      const msg = e instanceof Error ? e.message : 'Unknown error';
      set({ isSending: false, error: `Error: ${msg}` });
    }
  },

  _addOptimisticMessage: (sessionId, content) => {
    const msg: Message = {
      id: optimisticIdCounter--,
      role: 'USER',
      content,
      toolName: null,
      createdAt: new Date().toISOString(),
    };
    set(state => ({
      messages: {
        ...state.messages,
        [sessionId]: [...(state.messages[sessionId] ?? []), msg],
      },
    }));
  },

  _addAssistantMessage: (sessionId, content) => {
    const msg: Message = {
      id: optimisticIdCounter--,
      role: 'ASSISTANT',
      content,
      toolName: null,
      createdAt: new Date().toISOString(),
    };
    set(state => ({
      messages: {
        ...state.messages,
        [sessionId]: [...(state.messages[sessionId] ?? []), msg],
      },
    }));
  },

  _removeLastMessage: (sessionId) => {
    set(state => ({
      messages: {
        ...state.messages,
        [sessionId]: (state.messages[sessionId] ?? []).slice(0, -1),
      },
    }));
  },
}));
