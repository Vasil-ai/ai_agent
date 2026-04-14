import { useEffect } from 'react';
import { useChatStore } from '../store/chatStore';
import { PlusIcon, TrashIcon, ChatBubbleLeftIcon } from './icons';

export default function SessionList() {
  const { sessions, activeSessionId, isLoading, loadSessions, selectSession, createNewSession, deleteSession } =
    useChatStore();

  useEffect(() => {
    loadSessions();
  }, []);

  const handleNew = async () => {
    await createNewSession();
  };

  const handleDelete = async (e: React.MouseEvent, id: string) => {
    e.stopPropagation();
    if (confirm('Delete this conversation?')) {
      await deleteSession(id);
    }
  };

  const formatDate = (iso: string) => {
    const d = new Date(iso);
    const now = new Date();
    const diff = now.getTime() - d.getTime();
    if (diff < 60_000) return 'just now';
    if (diff < 3_600_000) return `${Math.floor(diff / 60_000)}m ago`;
    if (diff < 86_400_000) return `${Math.floor(diff / 3_600_000)}h ago`;
    return d.toLocaleDateString();
  };

  return (
    <aside className="flex flex-col h-full w-64 bg-gray-900 border-r border-gray-800">
      {/* Header */}
      <div className="flex items-center justify-between px-4 py-4 border-b border-gray-800">
        <div className="flex items-center gap-2">
          <div className="w-7 h-7 rounded-lg bg-brand-500 flex items-center justify-center text-white text-xs font-bold">
            AI
          </div>
          <span className="font-semibold text-gray-100 text-sm">AI Agent</span>
        </div>
        <button
          onClick={handleNew}
          title="New chat"
          className="p-1.5 rounded-lg text-gray-400 hover:text-white hover:bg-gray-700 transition-colors"
        >
          <PlusIcon className="w-4 h-4" />
        </button>
      </div>

      {/* Sessions list */}
      <div className="flex-1 overflow-y-auto scrollbar-thin py-2">
        {isLoading && sessions.length === 0 && (
          <div className="px-4 py-3 text-gray-500 text-sm">Loading...</div>
        )}

        {sessions.length === 0 && !isLoading && (
          <div className="px-4 py-6 text-center">
            <ChatBubbleLeftIcon className="w-8 h-8 text-gray-600 mx-auto mb-2" />
            <p className="text-gray-500 text-sm">No conversations yet</p>
            <button
              onClick={handleNew}
              className="mt-3 text-brand-500 text-sm hover:text-brand-600 transition-colors"
            >
              Start a new chat
            </button>
          </div>
        )}

        {sessions.map(session => (
          <div
            key={session.id}
            onClick={() => selectSession(session.id)}
            className={`group flex items-start gap-2 px-3 py-2.5 mx-2 rounded-lg cursor-pointer transition-colors ${
              session.id === activeSessionId
                ? 'bg-gray-700 text-white'
                : 'text-gray-400 hover:bg-gray-800 hover:text-gray-200'
            }`}
          >
            <ChatBubbleLeftIcon className="w-4 h-4 mt-0.5 flex-shrink-0 opacity-60" />
            <div className="flex-1 min-w-0">
              <p className="text-sm truncate leading-snug">
                {session.title || 'New conversation'}
              </p>
              <p className="text-xs text-gray-600 mt-0.5">
                {session.messageCount} msgs · {formatDate(session.updatedAt)}
              </p>
            </div>
            <button
              onClick={(e) => handleDelete(e, session.id)}
              className="opacity-0 group-hover:opacity-100 p-1 rounded hover:text-red-400 transition-all flex-shrink-0"
            >
              <TrashIcon className="w-3.5 h-3.5" />
            </button>
          </div>
        ))}
      </div>

      {/* Footer */}
      <div className="px-4 py-3 border-t border-gray-800">
        <p className="text-xs text-gray-600 text-center">
          Powered by llama.cpp · Ministral 3B
        </p>
      </div>
    </aside>
  );
}
