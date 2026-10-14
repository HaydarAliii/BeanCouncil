import type { ReactNode } from 'react';

type KeyInputPanelProps = {
  title: string;
  description: ReactNode;
  hasKey: boolean;
  keyPreview: string | null;
  value: string;
  onChange: (value: string) => void;
  emptyPlaceholder: string;
};

/** OpenRouter and Tavily key inputs share the same structure — merged into a single component. */
export function KeyInputPanel({
  title,
  description,
  hasKey,
  keyPreview,
  value,
  onChange,
  emptyPlaceholder,
}: KeyInputPanelProps) {
  return (
    <section className="settings-panel">
      <h3>{title}</h3>
      <p className="settings-hint">{description}</p>
      {hasKey && (
        <p className="key-status">
          Saved key: <code>{keyPreview}</code> — enter a new key below to change it.
        </p>
      )}
      <input
        type="password"
        placeholder={hasKey ? 'Enter a new key to change it (optional)' : emptyPlaceholder}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        autoComplete="off"
      />
    </section>
  );
}
