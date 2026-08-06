import { useState } from 'react';
import { askCouncil } from './api';
import type { CouncilResult } from './api';
import { PromptForm } from './components/PromptForm';
import { FinalAnswer } from './components/FinalAnswer';
import { ProcessDetails } from './components/ProcessDetails';

function App() {
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<CouncilResult | null>(null);
  const [error, setError] = useState<string | null>(null);

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

  return (
    <main className="app">
      <h1>LLM Council</h1>
      <p className="subtitle">
        Sorunu birden fazla AI modeline sor, birbirlerini değerlendirsinler, başkan sentezlesin.
      </p>

      <PromptForm loading={loading} onSubmit={handleSubmit} />

      {error && <p className="error-text">{error}</p>}

      {result && (
        <>
          <FinalAnswer result={result} />
          <ProcessDetails result={result} />
        </>
      )}
    </main>
  );
}

export default App;
