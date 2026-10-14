import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { SettingsPage } from './SettingsPage';
import * as api from '../api';

vi.mock('../api', () => ({
  getSettings: vi.fn(),
  getModels: vi.fn(),
  getKeyStatus: vi.fn(),
  saveSettings: vi.fn(),
}));

function settingsResponse(overrides: Partial<api.SettingsResponse> = {}): api.SettingsResponse {
  return {
    hasKey: true,
    keyPreview: '••••1234',
    hasTavilyKey: false,
    tavilyKeyPreview: null,
    selectedModelIds: ['a', 'b'],
    presidentModelId: 'a',
    ...overrides,
  };
}

const twoModels: api.ModelInfo[] = [
  { id: 'a', name: 'Model A', contextLength: null, promptPrice: '0', completionPrice: '0', isFree: true },
  { id: 'b', name: 'Model B', contextLength: null, promptPrice: '0', completionPrice: '0', isFree: true },
];

const freeKeyStatus: api.KeyStatus = { label: null, limit: null, usage: null, isFreeTier: true, limitRemaining: null };

describe('SettingsPage', () => {
  it('shows "Loading settings..." while loading', async () => {
    let resolveSettings!: (value: api.SettingsResponse) => void;
    vi.mocked(api.getSettings).mockReturnValue(new Promise((resolve) => (resolveSettings = resolve)));
    vi.mocked(api.getModels).mockResolvedValue([]);

    render(<SettingsPage onSaved={vi.fn()} />);

    expect(screen.getByText('Loading settings...')).toBeInTheDocument();
    resolveSettings(settingsResponse({ hasKey: false, keyPreview: null }));
    await screen.findByText('OpenRouter API Key');
  });

  it('shows error-text on load failure', async () => {
    vi.mocked(api.getSettings).mockRejectedValue(new Error('Could not load settings (HTTP 500)'));
    vi.mocked(api.getModels).mockResolvedValue([]);

    render(<SettingsPage onSaved={vi.fn()} />);

    expect(await screen.findByText('Could not load settings (HTTP 500)')).toBeInTheDocument();
  });

  it('disables Save and shows the model/president hint when fewer than 2 models are selected', async () => {
    vi.mocked(api.getSettings).mockResolvedValue(settingsResponse({ selectedModelIds: ['a'], presidentModelId: 'a' }));
    vi.mocked(api.getModels).mockResolvedValue([twoModels[0]]);
    vi.mocked(api.getKeyStatus).mockResolvedValue(freeKeyStatus);

    render(<SettingsPage onSaved={vi.fn()} />);

    const saveButton = await screen.findByRole('button', { name: 'Save' });
    expect(saveButton).toBeDisabled();
    expect(screen.getByText('Select at least 2 models and choose a president among them.')).toBeInTheDocument();
  });

  it('shows the "enter an OpenRouter key" hint when no key is saved and the input is empty, getKeyStatus is never called', async () => {
    vi.mocked(api.getSettings).mockResolvedValue(
      settingsResponse({ hasKey: false, keyPreview: null, selectedModelIds: ['a', 'b'], presidentModelId: 'a' }),
    );
    vi.mocked(api.getModels).mockResolvedValue(twoModels);

    render(<SettingsPage onSaved={vi.fn()} />);

    const saveButton = await screen.findByRole('button', { name: 'Save' });
    expect(saveButton).toBeDisabled();
    expect(screen.getByText('Enter an OpenRouter key to continue.')).toBeInTheDocument();
    expect(api.getKeyStatus).not.toHaveBeenCalled();
  });

  it('clears the key inputs after a successful save (write-only), shows a success message, calls onSaved', async () => {
    vi.mocked(api.getSettings).mockResolvedValue(settingsResponse());
    vi.mocked(api.getModels).mockResolvedValue(twoModels);
    vi.mocked(api.getKeyStatus).mockResolvedValue(freeKeyStatus);
    const savedResponse = settingsResponse({ keyPreview: '••••5678' });
    vi.mocked(api.saveSettings).mockResolvedValue(savedResponse);
    const onSaved = vi.fn();

    render(<SettingsPage onSaved={onSaved} />);
    const saveButton = await screen.findByRole('button', { name: 'Save' });
    expect(saveButton).toBeEnabled();

    const openRouterInput = screen.getByPlaceholderText('Enter a new key to change it (optional)');
    await userEvent.type(openRouterInput, 'new-key');
    await userEvent.click(saveButton);

    expect(await screen.findByText('Settings saved.')).toBeInTheDocument();
    expect(openRouterInput).toHaveValue('');
    expect(onSaved).toHaveBeenCalledWith(savedResponse);
  });

  it('shows error-text on save failure, onSaved is not called', async () => {
    vi.mocked(api.getSettings).mockResolvedValue(settingsResponse());
    vi.mocked(api.getModels).mockResolvedValue(twoModels);
    vi.mocked(api.getKeyStatus).mockResolvedValue(freeKeyStatus);
    vi.mocked(api.saveSettings).mockRejectedValue(new Error('Could not save settings (HTTP 500)'));
    const onSaved = vi.fn();

    render(<SettingsPage onSaved={onSaved} />);
    await userEvent.click(await screen.findByRole('button', { name: 'Save' }));

    expect(await screen.findByText('Could not save settings (HTTP 500)')).toBeInTheDocument();
    expect(onSaved).not.toHaveBeenCalled();
  });

  it('silently swallows a getKeyStatus failure during load, the page still renders', async () => {
    vi.mocked(api.getSettings).mockResolvedValue(settingsResponse());
    vi.mocked(api.getModels).mockResolvedValue(twoModels);
    vi.mocked(api.getKeyStatus).mockRejectedValue(new Error('Could not reach OpenRouter'));

    render(<SettingsPage onSaved={vi.fn()} />);

    expect(await screen.findByText('OpenRouter API Key')).toBeInTheDocument();
    expect(screen.queryByText(/Could not reach/)).not.toBeInTheDocument();
  });
});
