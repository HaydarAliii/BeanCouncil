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
      <h3>Council Members</h3>
      <p className="settings-hint">
        Select at least 2 models and make one of them president. The president doesn't give a first
        opinion or review the others — it just listens to everyone and makes the final call.
      </p>

      {keyStatus && (
        <p className="credit-status">
          {keyStatus.isFreeTier ? 'Free tier' : 'Paid account'}
          {keyStatus.limit != null && keyStatus.usage != null && (
            <> — credit: ${(keyStatus.limit - keyStatus.usage).toFixed(2)} / ${keyStatus.limit.toFixed(2)} left</>
          )}
        </p>
      )}

      <input
        className="model-filter"
        type="text"
        placeholder="Search models (e.g. claude, gemini, gpt, grok...)"
        value={filter}
        onChange={(e) => setFilter(e.target.value)}
      />

      <div className="model-list">
        {filtered.map((model) => {
          const isSelected = selected.includes(model.id);
          const isLocal = model.id.startsWith('ollama/');
          return (
            <label key={model.id} className={`model-row${isSelected ? ' model-row-selected' : ''}`}>
              <input type="checkbox" checked={isSelected} onChange={() => onToggle(model.id)} />
              <span className="model-name">{model.name}</span>
              <span className="model-id">{model.id}</span>
              {isLocal ? (
                <span className="model-badge badge-local">Local</span>
              ) : (
                <span className={`model-badge ${model.isFree ? 'badge-free' : 'badge-paid'}`}>
                  {model.isFree ? 'Free' : 'Paid'}
                </span>
              )}
              <input
                type="radio"
                name="president"
                checked={president === model.id}
                disabled={!isSelected}
                onChange={() => onSelectPresident(model.id)}
                title="Make president"
              />
            </label>
          );
        })}
        {filtered.length === 0 && <p className="settings-hint">No matching models found.</p>}
      </div>
    </section>
  );
}
