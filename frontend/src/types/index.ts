export interface Session {
  id: string;
  title: string;
  userId: string | null;
  messageCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface Message {
  id: number;
  role: 'USER' | 'ASSISTANT' | 'SYSTEM' | 'TOOL_CALL' | 'TOOL_RESULT';
  content: string;
  toolName: string | null;
  createdAt: string;
}

export interface ChatResponse {
  sessionId: string;
  message: string;
  timestamp: string;
}

export interface SendMessagePayload {
  message: string;
  userId?: string;
}
