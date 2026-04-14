import { useEffect, useRef } from 'react';
import { useChatStore } from '../store/chatStore';
import MessageBubble from './MessageBubble';
import ChatInput from './ChatInput';
import { SparklesIcon } from './icons';

const SUGGESTED_PROMPTS = [
  'What is 2847 multiplied by 193?',
  'What is today\'s date and time?',
  'What is the capital of France?',
  'Search for latest news about artificial intelligence',
];

function EmptyState({ onPrompt }: { onPrompt: (text: string) => void }) {
  return (
    <div className="flex-1 flex flex-col items-center justify-center px-6 py-12">
      <div className="w-14 h-14 rounded-2xl bg-brand-500/10 border border-brand-500/20 flex items-center justify-center mb-5">
        <SparklesIcon className="w-7 h-7 text-brand-500" />
      </div>
      <h2 className="text-xl font-semibold text-gray-100 mb-2">How can I help you?</h2>
      <p className="text-gray-500 text-sm text-center mb-8 max-w-sm">
        I'm an AI agent with tools for calculations, real-time info, and web search.
      </p>
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 w-full max-w-xl">
        {SUGGESTED_PROMPTS.map(prompt => (
          <button
            key={prompt}
            onClick={() => onPrompt(prompt)}
            className="text-left px-4 py-3 rounded-xl bg-gray-800 hover:bg-gray-700 border border-gray-700 hover:border-gray-600 text-sm text-gray-300 transition-colors"
          >
            {prompt}
          </button>
        ))}
      </div>
    </div>
  );
}

export default function ChatWindow() {
  const { activeSessionId, messages, isSending, error, sendMessage, clearError } = useChatStore();
  const bottomRef = useRef<HTMLDivElement>(null);

  const currentMessages = activeSessionId ? (messages[activeSessionId] ?? []) : [];

  // Авто-скролл к последнему сообщению
  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [currentMessages.length]);

  const handleSuggestedPrompt = (text: string) => {
    sendMessage(text);
  };

  return (
    <div className="flex flex-col flex-1 h-full min-w-0">
      {/* Header */}
      <div className="flex items-center justify-between px-6 py-4 border-b border-gray-800 bg-gray-950/80 backdrop-blur-sm">
        <div>
          <h1 className="font-semibold text-gray-100 text-base">
            {activeSessionId
              ? 'AI Agent Chat'
              : 'New Conversation'}
          </h1>
          <p className="text-xs text-gray-500 mt-0.5">
            Ministral 3B · Tools: calculator, datetime, web_search
          </p>
        </div>
        {isSending && (
          <div className="flex items-center gap-2 text-xs text-brand-400">
            <div className="w-3 h-3 border-2 border-brand-400/30 border-t-brand-400 rounded-full animate-spin" />
            Thinking...
          </div>
        )}
      </div>

      {/* Messages */}
      <div className="flex-1 overflow-y-auto scrollbar-thin px-4 py-6">
        {error && (
          <div className="max-w-3xl mx-auto mb-4 px-4 py-3 bg-red-500/10 border border-red-500/30 rounded-xl text-red-400 text-sm flex items-center justify-between">
            <span>{error}</span>
            <button onClick={clearError} className="ml-3 text-red-400 hover:text-red-300 font-bold">×</button>
          </div>
        )}

        {currentMessages.length === 0 ? (
          <EmptyState onPrompt={handleSuggestedPrompt} />
        ) : (
          <div className="max-w-3xl mx-auto space-y-6">
            {currentMessages.map(msg => (
              <MessageBubble key={msg.id} message={msg} />
            ))}
            <div ref={bottomRef} />
          </div>
        )}
      </div>

      {/* Input */}
      <ChatInput />
    </div>
  );
}
