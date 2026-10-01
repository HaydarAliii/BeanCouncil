import { useEffect, useState } from 'react';
import { getConversationDetail, getConversations } from '../api';
import type { ConversationSummary, CouncilResult } from '../api';
import { FinalAnswer } from './FinalAnswer';
import { ProcessDetails } from './ProcessDetails';

export function HistoryPage() {
  const [conversations, setConversations] = useState<ConversationSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [detail, setDetail] = useState<CouncilResult | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState<string | null>(null);

  useEffect(() => {
    getConversations()
      .then(setConversations)
      .catch((err) => setError(err instanceof Error ? err.message : 'Geçmiş alınamadı.'))
      .finally(() => setLoading(false));
  }, []);

  async function openConversation(id: number) {
    setSelectedId(id);
    setDetail(null);
    setDetailError(null);
    setDetailLoading(true);
    try {
      setDetail(await getConversationDetail(id));
    } catch (err) {
      setDetailError(err instanceof Error ? err.message : 'Konuşma alınamadı.');
    } finally {
      setDetailLoading(false);
    }
  }

  if (selectedId !== null) {
    return (
      <div className="history-page">
        <button className="history-back-button" onClick={() => setSelectedId(null)}>
          ← Geçmişe dön
        </button>
        {detailLoading && <p className="settings-hint">Yükleniyor...</p>}
        {detailError && <p className="error-text">{detailError}</p>}
        {detail && (
          <>
            <FinalAnswer result={detail} />
            <ProcessDetails result={detail} />
          </>
        )}
      </div>
    );
  }

  if (loading) return <p className="settings-hint">Geçmiş yükleniyor...</p>;
  if (error) return <p className="error-text">{error}</p>;
  if (conversations.length === 0) {
    return <p className="settings-hint">Henüz hiç soru sorulmamış.</p>;
  }

  return (
    <div className="history-page">
      <ul className="history-list">
        {conversations.map((c) => (
          <li key={c.id}>
            <button className="history-item" onClick={() => openConversation(c.id)}>
              <span className="history-item-prompt">{c.prompt}</span>
              <span className="history-item-date">
                {new Date(c.createdAt).toLocaleString('tr-TR')}
              </span>
            </button>
          </li>
        ))}
      </ul>
    </div>
  );
}
