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
import { KeyInputPanel } from './KeyInputPanel';
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
            // key is saved but OpenRouter is unreachable/invalid — fail silently, user can still adjust settings
          }
        }
      } catch (err) {
        if (!cancelled) setLoadError(err instanceof Error ? err.message : 'Could not load settings.');
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
      setSaveError(err instanceof Error ? err.message : 'Could not save settings.');
    } finally {
      setSaving(false);
    }
  }

  if (loading) return <p className="settings-hint">Loading settings...</p>;
  if (loadError) return <p className="error-text">{loadError}</p>;

  return (
    <div className="settings-page">
      <KeyInputPanel
        title="OpenRouter API Key"
        description="Your key is stored encrypted on the backend and is never shown back in full."
        hasKey={settings?.hasKey ?? false}
        keyPreview={settings?.keyPreview ?? null}
        value={keyInput}
        onChange={setKeyInput}
        emptyPlaceholder="sk-or-v1-..."
      />

      <KeyInputPanel
        title="Web Search (optional)"
        description={
          <>
            Grab a free key from{' '}
            <a href="https://app.tavily.com/home" target="_blank" rel="noreferrer">
              Tavily
            </a>{' '}
            and enter it to ask the council with the "Search the web" option — members will factor in
            live search results when answering. Leave it blank and this feature stays hidden.
          </>
        }
        hasKey={settings?.hasTavilyKey ?? false}
        keyPreview={settings?.tavilyKeyPreview ?? null}
        value={tavilyKeyInput}
        onChange={setTavilyKeyInput}
        emptyPlaceholder="tvly-..."
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
      {saveSuccess && <p className="save-success">Settings saved.</p>}

      <button className="save-settings-button" disabled={!canSave || saving} onClick={handleSave}>
        {saving ? 'Saving...' : 'Save'}
      </button>
      {!canSave && (
        <p className="settings-hint">
          {needsNewKey
            ? 'Enter an OpenRouter key to continue.'
            : 'Select at least 2 models and choose a president among them.'}
        </p>
      )}
    </div>
  );
}
