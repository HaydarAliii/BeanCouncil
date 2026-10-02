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

/** OpenRouter ve Tavily key girişleri aynı yapıyı paylaşır — tek bileşende birleştirildi. */
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
          Kayıtlı key: <code>{keyPreview}</code> — değiştirmek için aşağıya yeni bir key gir.
        </p>
      )}
      <input
        type="password"
        placeholder={hasKey ? 'Değiştirmek için yeni key gir (opsiyonel)' : emptyPlaceholder}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        autoComplete="off"
      />
    </section>
  );
}
