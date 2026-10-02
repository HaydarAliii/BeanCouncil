import { useState } from 'react';
import type { FormEvent } from 'react';

type PromptFormProps = {
  loading: boolean;
  onSubmit: (prompt: string, webSearch: boolean) => void;
  placeholder?: string;
  webSearchAvailable?: boolean;
};

export function PromptForm({ loading, onSubmit, placeholder, webSearchAvailable }: PromptFormProps) {
  const [prompt, setPrompt] = useState('');
  const [webSearch, setWebSearch] = useState(false);

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    const trimmed = prompt.trim();
    if (!trimmed || loading) {
      return;
    }
    onSubmit(trimmed, webSearch);
    setPrompt('');
  }

  return (
    <form className="prompt-form" onSubmit={handleSubmit}>
      <textarea
        value={prompt}
        onChange={(event) => setPrompt(event.target.value)}
        placeholder={placeholder ?? 'Konseye sormak istediğin soruyu yaz...'}
        rows={4}
        disabled={loading}
      />
      <div className="prompt-form-actions">
        <button type="submit" disabled={loading || !prompt.trim()}>
          {loading ? 'Konsey görüşüyor…' : 'Konseye Sor'}
        </button>
        {webSearchAvailable && (
          <label className="web-search-toggle">
            <input
              type="checkbox"
              checked={webSearch}
              onChange={(e) => setWebSearch(e.target.checked)}
              disabled={loading}
            />
            🔍 Web'de ara
          </label>
        )}
      </div>
    </form>
  );
}
