import { useEffect, useState } from 'react';
import { askCouncil, getSettings } from './api';
import type { CouncilResult, SettingsResponse } from './api';
import { PromptForm } from './components/PromptForm';
import { FinalAnswer } from './components/FinalAnswer';
import { ProcessDetails } from './components/ProcessDetails';
import { SettingsPage } from './components/SettingsPage';
import { HistoryPage } from './components/HistoryPage';

type View = 'council' | 'settings' | 'history';

function App() {
  const [view, setView] = useState<View>('council');
  const [configured, setConfigured] = useState<boolean | null>(null);
  const [webSearchAvailable, setWebSearchAvailable] = useState(false);
  const [loading, setLoading] = useState(false);
  const [threadId, setThreadId] = useState<number | null>(null);
  const [turns, setTurns] = useState<CouncilResult[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getSettings()
      .then((settings) => {
        const ready = settings.hasKey && settings.selectedModelIds.length >= 2 && !!settings.presidentModelId;
        setConfigured(ready);
        setWebSearchAvailable(settings.hasTavilyKey);
        if (!ready) setView('settings');
      })
      .catch(() => setConfigured(false));
  }, []);

  async function handleSubmit(prompt: string, webSearch: boolean) {
    setLoading(true);
    setError(null);
    try {
      const councilResult = await askCouncil(prompt, threadId, webSearch);
      setTurns((prev) => [...prev, councilResult]);
      setThreadId(councilResult.threadId);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'An unexpected error occurred.');
    } finally {
      setLoading(false);
    }
  }

  function handleNewConversation() {
    setThreadId(null);
    setTurns([]);
    setError(null);
  }

  function handleSettingsSaved(settings: SettingsResponse) {
    setConfigured(true);
    setWebSearchAvailable(settings.hasTavilyKey);
    setView('council');
  }

  function handleContinueFromHistory(id: number, loadedTurns: CouncilResult[]) {
    setThreadId(id);
    setTurns(loadedTurns);
    setError(null);
    setView('council');
  }

  return (
    <main className="app">
      <div className="app-header">
        <h1>BeanCouncil</h1>
        <nav className="view-tabs">
          <button className={view === 'council' ? 'active' : ''} onClick={() => setView('council')}>
            Council
          </button>
          <button className={view === 'settings' ? 'active' : ''} onClick={() => setView('settings')}>
            Settings
          </button>
          <button className={view === 'history' ? 'active' : ''} onClick={() => setView('history')}>
            History
          </button>
        </nav>
      </div>

      {view === 'settings' && <SettingsPage onSaved={handleSettingsSaved} />}

      {view === 'history' && <HistoryPage onContinue={handleContinueFromHistory} />}

      {view === 'council' && (
        <>
          <p className="subtitle">
            Ask one question to multiple AI models, let them review each other's answers, and have the
            president synthesize the final one.
          </p>

          {configured === false && (
            <p className="error-text">
              Setup isn't finished yet. Head to the Settings tab, enter your OpenRouter key, and pick your
              council members.
            </p>
          )}

          {turns.length > 0 && (
            <button className="new-conversation-button" onClick={handleNewConversation}>
              + New conversation
            </button>
          )}

          <PromptForm
            loading={loading}
            onSubmit={handleSubmit}
            placeholder={turns.length > 0 ? 'Write a follow-up question...' : undefined}
            webSearchAvailable={webSearchAvailable}
          />

          {error && <p className="error-text">{error}</p>}

          {turns.map((turn, index) => (
            <div className="conversation-turn" key={index}>
              <FinalAnswer result={turn} />
              <ProcessDetails result={turn} />
            </div>
          ))}
        </>
      )}
    </main>
  );
}

export default App;
