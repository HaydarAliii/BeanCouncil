import { useMemo, useState } from 'react';
import type { KeyStatus, ModelInfo } from '../api';

type ModelPickerProps = {
  models: ModelInfo[];
  keyStatus: KeyStatus | null;
  selected: string[];
  president: string | null;
  onToggle: (modelId: string) => void;
  onSelectPresident: (modelId: string) => void;
};

export function ModelPicker({ models, keyStatus, selected, president, onToggle, onSelectPresident }: ModelPickerProps) {
  const [filter, setFilter] = useState('');

  const filtered = useMemo(() => {
    const q = filter.trim().toLowerCase();
    if (!q) return models;
    return models.filter((m) => m.id.toLowerCase().includes(q) || m.name.toLowerCase().includes(q));
  }, [models, filter]);

  return (
    <section className="model-picker">
      <h3>Konsey Üyeleri</h3>
      <p className="settings-hint">
        En az 2 model seç, aralarından birini başkan yap. Başkan ilk görüş vermez/eleştiri yapmaz —
        sadece diğerlerini dinleyip nihai kararı verir.
      </p>

      {keyStatus && (
        <p className="credit-status">
          {keyStatus.isFreeTier ? 'Ücretsiz katman' : 'Ücretli hesap'}
          {keyStatus.limit != null && keyStatus.usage != null && (
            <> — kredi: {(keyStatus.limit - keyStatus.usage).toFixed(2)} / {keyStatus.limit.toFixed(2)} $ kaldı</>
          )}
        </p>
      )}

      <input
        className="model-filter"
        type="text"
        placeholder="Model ara (ör. claude, gemini, gpt, grok...)"
        value={filter}
        onChange={(e) => setFilter(e.target.value)}
      />

      <div className="model-list">
        {filtered.map((model) => {
          const isSelected = selected.includes(model.id);
          return (
            <label key={model.id} className={`model-row${isSelected ? ' model-row-selected' : ''}`}>
              <input type="checkbox" checked={isSelected} onChange={() => onToggle(model.id)} />
              <span className="model-name">{model.name}</span>
              <span className="model-id">{model.id}</span>
              <span className={`model-badge ${model.isFree ? 'badge-free' : 'badge-paid'}`}>
                {model.isFree ? 'Ücretsiz' : 'Ücretli'}
              </span>
              <input
                type="radio"
                name="president"
                checked={president === model.id}
                disabled={!isSelected}
                onChange={() => onSelectPresident(model.id)}
                title="Başkan yap"
              />
            </label>
          );
        })}
        {filtered.length === 0 && <p className="settings-hint">Eşleşen model bulunamadı.</p>}
      </div>
    </section>
  );
}
