type SettingsPanelProps = {
  hasKey: boolean;
  keyPreview: string | null;
  value: string;
  onChange: (value: string) => void;
};

export function SettingsPanel({ hasKey, keyPreview, value, onChange }: SettingsPanelProps) {
  return (
    <section className="settings-panel">
      <h3>OpenRouter API Key</h3>
      <p className="settings-hint">
        Key'in sadece backend'de şifreli saklanır, hiçbir zaman tam haliyle geri gösterilmez.
      </p>
      {hasKey && (
        <p className="key-status">
          Kayıtlı key: <code>{keyPreview}</code> — değiştirmek için aşağıya yeni bir key gir.
        </p>
      )}
      <input
        type="password"
        placeholder={hasKey ? 'Değiştirmek için yeni key gir (opsiyonel)' : 'sk-or-v1-...'}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        autoComplete="off"
      />
    </section>
  );
}
