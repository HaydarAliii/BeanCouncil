import { useEffect, useState } from 'react';
import { getConversationDetail, getConversations } from '../api';
import type { ConversationSummary, CouncilResult } from '../api';
import { FinalAnswer } from './FinalAnswer';
import { ProcessDetails } from './ProcessDetails';

type HistoryPageProps = {
  onContinue: (threadId: number, turns: CouncilResult[]) => void;
};

export function HistoryPage({ onContinue }: HistoryPageProps) {
  const [conversations, setConversations] = useState<ConversationSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [turns, setTurns] = useState<CouncilResult[] | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState<string | null>(null);

  useEffect(() => {
    getConversations()
      .then(setConversations)
      .catch((err) => setError(err instanceof Error ? err.message : 'Could not load history.'))
      .finally(() => setLoading(false));
  }, []);

  async function openConversation(id: number) {
    setSelectedId(id);
    setTurns(null);
    setDetailError(null);
    setDetailLoading(true);
    try {
      setTurns(await getConversationDetail(id));
    } catch (err) {
      setDetailError(err instanceof Error ? err.message : 'Could not load conversation.');
    } finally {
      setDetailLoading(false);
    }
  }

  if (selectedId !== null) {
    return (
      <div className="history-page">
        <div className="history-detail-header">
          <button className="history-back-button" onClick={() => setSelectedId(null)}>
            ← Back to history
          </button>
          {turns && (
            <button className="history-continue-button" onClick={() => onContinue(selectedId, turns)}>
              Continue this conversation →
            </button>
          )}
        </div>
        {detailLoading && <p className="settings-hint">Loading...</p>}
        {detailError && <p className="error-text">{detailError}</p>}
        {turns?.map((turn, index) => (
          <div className="conversation-turn" key={index}>
            <FinalAnswer result={turn} />
            <ProcessDetails result={turn} />
          </div>
        ))}
      </div>
    );
  }

  if (loading) return <p className="settings-hint">Loading history...</p>;
  if (error) return <p className="error-text">{error}</p>;
  if (conversations.length === 0) {
    return <p className="settings-hint">No questions asked yet.</p>;
  }

  return (
    <div className="history-page">
      <ul className="history-list">
        {conversations.map((c) => (
          <li key={c.id}>
            <button className="history-item" onClick={() => openConversation(c.id)}>
              <span className="history-item-prompt">{c.title}</span>
              <span className="history-item-date">
                {new Date(c.updatedAt).toLocaleString('en-US')}
                {c.turnCount > 1 ? ` · ${c.turnCount} turns` : ''}
              </span>
            </button>
          </li>
        ))}
      </ul>
    </div>
  );
}
