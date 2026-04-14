import { useState, useRef, useEffect } from 'react';
import { useChatStore } from '../store/chatStore';
import { SendIcon } from './icons';

export default function ChatInput() {
  const [text, setText] = useState('');
  const { isSending, sendMessage } = useChatStore();
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  // Авторазмер textarea
  useEffect(() => {
    const ta = textareaRef.current;
    if (!ta) return;
    ta.style.height = 'auto';
    ta.style.height = Math.min(ta.scrollHeight, 180) + 'px';
  }, [text]);

  const handleSend = async () => {
    const trimmed = text.trim();
    if (!trimmed || isSending) return;
    setText('');
    await sendMessage(trimmed);
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  return (
    <div className="px-4 py-4 border-t border-gray-800 bg-gray-950">
      <div className="max-w-3xl mx-auto">
        <div className={`flex items-end gap-3 bg-gray-800 border rounded-2xl px-4 py-3 transition-colors ${
          isSending ? 'border-gray-700' : 'border-gray-600 focus-within:border-brand-500'
        }`}>
          <textarea
            ref={textareaRef}
            value={text}
            onChange={e => setText(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder={isSending ? 'Agent is thinking...' : 'Message AI Agent... (Enter to send, Shift+Enter for new line)'}
            disabled={isSending}
            rows={1}
            className="flex-1 bg-transparent text-gray-100 placeholder-gray-500 text-sm resize-none outline-none leading-relaxed disabled:opacity-50"
          />
          <button
            onClick={handleSend}
            disabled={!text.trim() || isSending}
            className="flex-shrink-0 w-8 h-8 rounded-xl bg-brand-500 hover:bg-brand-600 disabled:opacity-30 disabled:cursor-not-allowed flex items-center justify-center transition-colors"
          >
            {isSending ? (
              <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
            ) : (
              <SendIcon className="w-4 h-4 text-white" />
            )}
          </button>
        </div>
        <p className="text-xs text-gray-700 text-center mt-2">
          AI can make mistakes. Verify important information.
        </p>
      </div>
    </div>
  );
}
