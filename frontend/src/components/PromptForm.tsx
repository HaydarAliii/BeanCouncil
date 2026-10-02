import { useState } from 'react';
import type { FormEvent } from 'react';

type PromptFormProps = {
  loading: boolean;
  onSubmit: (prompt: string) => void;
  placeholder?: string;
};

export function PromptForm({ loading, onSubmit, placeholder }: PromptFormProps) {
  const [prompt, setPrompt] = useState('');

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    const trimmed = prompt.trim();
    if (!trimmed || loading) {
      return;
    }
    onSubmit(trimmed);
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
      <button type="submit" disabled={loading || !prompt.trim()}>
        {loading ? 'Konsey görüşüyor…' : 'Konseye Sor'}
      </button>
    </form>
  );
}
