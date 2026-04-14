import SessionList from './components/SessionList';
import ChatWindow from './components/ChatWindow';

export default function App() {
  return (
    <div className="flex h-full bg-gray-950">
      <SessionList />
      <ChatWindow />
    </div>
  );
}
