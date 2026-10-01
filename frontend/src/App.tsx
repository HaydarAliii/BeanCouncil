import { useEffect, useState } from 'react';
import { askCouncil, getSettings } from './api';
import type { CouncilResult } from './api';
import { PromptForm } from './components/PromptForm';
import { FinalAnswer } from './components/FinalAnswer';
import { ProcessDetails } from './components/ProcessDetails';
import { SettingsPage } from './components/SettingsPage';
import { HistoryPage } from './components/HistoryPage';

type View = 'council' | 'settings' | 'history';

function App() {
  const [view, setView] = useState<View>('council');
  const [configured, setConfigured] = useState<boolean | null>(null);
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<CouncilResult | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getSettings()
      .then((settings) => {
        const ready = settings.hasKey && settings.selectedModelIds.length >= 2 && !!settings.presidentModelId;
        setConfigured(ready);
        if (!ready) setView('settings');
      })
      .catch(() => setConfigured(false));
  }, []);

  async function handleSubmit(prompt: string) {
    setLoading(true);
    setError(null);
    setResult(null);
    try {
      const councilResult = await askCouncil(prompt);
      setResult(councilResult);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Beklenmeyen bir hata oluştu.');
    } finally {
      setLoading(false);
    }
  }

  function handleSettingsSaved() {
    setConfigured(true);
    setView('council');
  }

  return (
    <main className="app">
      <div className="app-header">
        <h1>BeanCouncil</h1>
        <nav className="view-tabs">
          <button className={view === 'council' ? 'active' : ''} onClick={() => setView('council')}>
            Konsey
          </button>
          <button className={view === 'settings' ? 'active' : ''} onClick={() => setView('settings')}>
            Ayarlar
          </button>
          <button className={view === 'history' ? 'active' : ''} onClick={() => setView('history')}>
            Geçmiş
          </button>
        </nav>
      </div>

      {view === 'settings' && <SettingsPage onSaved={handleSettingsSaved} />}

      {view === 'history' && <HistoryPage />}

      {view === 'council' && (
        <>
          <p className="subtitle">
            Sorunu birden fazla AI modeline sor, birbirlerini değerlendirsinler, başkan sentezlesin.
          </p>

          {configured === false && (
            <p className="error-text">
              Henüz yapılandırma tamamlanmadı. Lütfen Ayarlar sekmesinden OpenRouter key'ini gir ve konsey
              üyelerini seç.
            </p>
          )}

          <PromptForm loading={loading} onSubmit={handleSubmit} />

          {error && <p className="error-text">{error}</p>}

          {result && (
            <>
              <FinalAnswer result={result} />
              <ProcessDetails result={result} />
            </>
          )}
        </>
      )}
    </main>
  );
}

export default App;
