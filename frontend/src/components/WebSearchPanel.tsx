type WebSearchPanelProps = {
  hasKey: boolean;
  keyPreview: string | null;
  value: string;
  onChange: (value: string) => void;
};

export function WebSearchPanel({ hasKey, keyPreview, value, onChange }: WebSearchPanelProps) {
  return (
    <section className="settings-panel">
      <h3>Web Araştırması (opsiyonel)</h3>
      <p className="settings-hint">
        <a href="https://app.tavily.com/home" target="_blank" rel="noreferrer">
          Tavily
        </a>
        'den ücretsiz bir key alıp girersen, konseye "Web'de ara" seçeneğiyle soru sorabilirsin —
        üyeler güncel arama sonuçlarını da görerek cevap verir. Boş bırakırsan bu özellik gizli kalır.
      </p>
      {hasKey && (
        <p className="key-status">
          Kayıtlı key: <code>{keyPreview}</code> — değiştirmek için aşağıya yeni bir key gir.
        </p>
      )}
      <input
        type="password"
        placeholder={hasKey ? 'Değiştirmek için yeni key gir (opsiyonel)' : 'tvly-...'}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        autoComplete="off"
      />
    </section>
  );
}
