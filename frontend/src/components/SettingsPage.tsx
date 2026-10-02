import { useEffect, useState } from 'react';
import {
  getKeyStatus,
  getModels,
  getSettings,
  saveSettings,
  type KeyStatus,
  type ModelInfo,
  type SettingsResponse,
} from '../api';
import { SettingsPanel } from './SettingsPanel';
import { WebSearchPanel } from './WebSearchPanel';
import { ModelPicker } from './ModelPicker';

type SettingsPageProps = {
  onSaved: (settings: SettingsResponse) => void;
};

export function SettingsPage({ onSaved }: SettingsPageProps) {
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  const [settings, setSettings] = useState<SettingsResponse | null>(null);
  const [models, setModels] = useState<ModelInfo[]>([]);
  const [keyStatus, setKeyStatus] = useState<KeyStatus | null>(null);

  const [keyInput, setKeyInput] = useState('');
  const [tavilyKeyInput, setTavilyKeyInput] = useState('');
  const [selectedModelIds, setSelectedModelIds] = useState<string[]>([]);
  const [presidentModelId, setPresidentModelId] = useState<string | null>(null);

  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState<string | null>(null);
  const [saveSuccess, setSaveSuccess] = useState(false);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      try {
        const [settingsRes, modelsRes] = await Promise.all([getSettings(), getModels()]);
        if (cancelled) return;
        setSettings(settingsRes);
        setModels(modelsRes);
        setSelectedModelIds(settingsRes.selectedModelIds);
        setPresidentModelId(settingsRes.presidentModelId);

        if (settingsRes.hasKey) {
          try {
            const status = await getKeyStatus();
            if (!cancelled) setKeyStatus(status);
          } catch {
            // key kayıtlı ama OpenRouter'a ulaşılamadı/geçersiz — sessiz geç, kullanıcı yine de ayar yapabilir
          }
        }
      } catch (err) {
        if (!cancelled) setLoadError(err instanceof Error ? err.message : 'Ayarlar yüklenemedi.');
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    load();
    return () => {
      cancelled = true;
    };
  }, []);

  function toggleModel(modelId: string) {
    setSelectedModelIds((prev) => {
      const next = prev.includes(modelId) ? prev.filter((id) => id !== modelId) : [...prev, modelId];
      if (presidentModelId && !next.includes(presidentModelId)) {
        setPresidentModelId(null);
      }
      return next;
    });
  }

  const needsNewKey = !settings?.hasKey && keyInput.trim() === '';
  const canSave = !needsNewKey && selectedModelIds.length >= 2 && presidentModelId !== null;

  async function handleSave() {
    if (!canSave || presidentModelId === null) return;
    setSaving(true);
    setSaveError(null);
    setSaveSuccess(false);
    try {
      const response = await saveSettings({
        openRouterKey: keyInput.trim() || undefined,
        tavilyKey: tavilyKeyInput.trim() || undefined,
        selectedModelIds,
        presidentModelId,
      });
      setSettings(response);
      setKeyInput('');
      setTavilyKeyInput('');
      setSaveSuccess(true);
      try {
        setKeyStatus(await getKeyStatus());
      } catch {
        setKeyStatus(null);
      }
      onSaved(response);
    } catch (err) {
      setSaveError(err instanceof Error ? err.message : 'Ayarlar kaydedilemedi.');
    } finally {
      setSaving(false);
    }
  }

  if (loading) return <p className="settings-hint">Ayarlar yükleniyor...</p>;
  if (loadError) return <p className="error-text">{loadError}</p>;

  return (
    <div className="settings-page">
      <SettingsPanel
        hasKey={settings?.hasKey ?? false}
        keyPreview={settings?.keyPreview ?? null}
        value={keyInput}
        onChange={setKeyInput}
      />

      <WebSearchPanel
        hasKey={settings?.hasTavilyKey ?? false}
        keyPreview={settings?.tavilyKeyPreview ?? null}
        value={tavilyKeyInput}
        onChange={setTavilyKeyInput}
      />

      <ModelPicker
        models={models}
        keyStatus={keyStatus}
        selected={selectedModelIds}
        president={presidentModelId}
        onToggle={toggleModel}
        onSelectPresident={setPresidentModelId}
      />

      {saveError && <p className="error-text">{saveError}</p>}
      {saveSuccess && <p className="save-success">Ayarlar kaydedildi.</p>}

      <button className="save-settings-button" disabled={!canSave || saving} onClick={handleSave}>
        {saving ? 'Kaydediliyor...' : 'Kaydet'}
      </button>
      {!canSave && (
        <p className="settings-hint">
          {needsNewKey
            ? 'Devam etmek için OpenRouter key gir.'
            : 'En az 2 model seç ve aralarından bir başkan belirle.'}
        </p>
      )}
    </div>
  );
}
