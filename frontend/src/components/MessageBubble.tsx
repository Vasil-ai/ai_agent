import ReactMarkdown from 'react-markdown';
import type { Message } from '../types';
import { BotIcon, UserIcon } from './icons';

interface Props {
  message: Message;
}

function TypingIndicator() {
  return (
    <div className="flex items-end gap-3">
      <div className="w-8 h-8 rounded-full bg-brand-500/20 border border-brand-500/30 flex items-center justify-center flex-shrink-0">
        <BotIcon className="w-4 h-4 text-brand-500" />
      </div>
      <div className="bg-gray-800 border border-gray-700 rounded-2xl rounded-bl-sm px-4 py-3">
        <div className="flex gap-1 items-center h-5">
          <span className="w-2 h-2 bg-gray-400 rounded-full animate-bounce [animation-delay:-0.3s]" />
          <span className="w-2 h-2 bg-gray-400 rounded-full animate-bounce [animation-delay:-0.15s]" />
          <span className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" />
        </div>
      </div>
    </div>
  );
}

export default function MessageBubble({ message }: Props) {
  const isUser = message.role === 'USER';
  const isTyping = message.content === '__typing__';

  if (isTyping) return <TypingIndicator />;

  if (isUser) {
    return (
      <div className="flex items-end gap-3 flex-row-reverse">
        <div className="w-8 h-8 rounded-full bg-gray-700 border border-gray-600 flex items-center justify-center flex-shrink-0">
          <UserIcon className="w-4 h-4 text-gray-300" />
        </div>
        <div className="max-w-[75%] bg-brand-600 text-white rounded-2xl rounded-br-sm px-4 py-2.5 shadow-sm">
          <p className="text-sm leading-relaxed whitespace-pre-wrap">{message.content}</p>
        </div>
      </div>
    );
  }

  return (
    <div className="flex items-end gap-3">
      <div className="w-8 h-8 rounded-full bg-brand-500/20 border border-brand-500/30 flex items-center justify-center flex-shrink-0">
        <BotIcon className="w-4 h-4 text-brand-500" />
      </div>
      <div className="max-w-[75%] bg-gray-800 border border-gray-700 rounded-2xl rounded-bl-sm px-4 py-2.5 shadow-sm">
        <div className="text-sm leading-relaxed text-gray-100 prose prose-invert prose-sm max-w-none
          prose-p:my-1 prose-pre:bg-gray-900 prose-pre:border prose-pre:border-gray-600
          prose-code:text-brand-400 prose-code:bg-gray-900 prose-code:px-1 prose-code:rounded
          prose-strong:text-white prose-headings:text-white">
          <ReactMarkdown>{message.content}</ReactMarkdown>
        </div>
        <p className="text-xs text-gray-600 mt-1.5 text-right">
          {new Date(message.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
        </p>
      </div>
    </div>
  );
}
