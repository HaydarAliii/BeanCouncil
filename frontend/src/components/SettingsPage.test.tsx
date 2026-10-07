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
  it('yüklenirken "Ayarlar yükleniyor..." gösterir', async () => {
    let resolveSettings!: (value: api.SettingsResponse) => void;
    vi.mocked(api.getSettings).mockReturnValue(new Promise((resolve) => (resolveSettings = resolve)));
    vi.mocked(api.getModels).mockResolvedValue([]);

    render(<SettingsPage onSaved={vi.fn()} />);

    expect(screen.getByText('Ayarlar yükleniyor...')).toBeInTheDocument();
    resolveSettings(settingsResponse({ hasKey: false, keyPreview: null }));
    await screen.findByText('OpenRouter API Key');
  });

  it('yükleme hatasında error-text gösterir', async () => {
    vi.mocked(api.getSettings).mockRejectedValue(new Error('Ayarlar alınamadı (HTTP 500)'));
    vi.mocked(api.getModels).mockResolvedValue([]);

    render(<SettingsPage onSaved={vi.fn()} />);

    expect(await screen.findByText('Ayarlar alınamadı (HTTP 500)')).toBeInTheDocument();
  });

  it("2'den az model seçiliyken Kaydet disabled olur ve model/başkan hint'i gösterilir", async () => {
    vi.mocked(api.getSettings).mockResolvedValue(settingsResponse({ selectedModelIds: ['a'], presidentModelId: 'a' }));
    vi.mocked(api.getModels).mockResolvedValue([twoModels[0]]);
    vi.mocked(api.getKeyStatus).mockResolvedValue(freeKeyStatus);

    render(<SettingsPage onSaved={vi.fn()} />);

    const saveButton = await screen.findByRole('button', { name: 'Kaydet' });
    expect(saveButton).toBeDisabled();
    expect(screen.getByText('En az 2 model seç ve aralarından bir başkan belirle.')).toBeInTheDocument();
  });

  it('key kayıtlı değilken ve input boşken "OpenRouter key gir" hint\'i gösterilir, getKeyStatus hiç çağrılmaz', async () => {
    vi.mocked(api.getSettings).mockResolvedValue(
      settingsResponse({ hasKey: false, keyPreview: null, selectedModelIds: ['a', 'b'], presidentModelId: 'a' }),
    );
    vi.mocked(api.getModels).mockResolvedValue(twoModels);

    render(<SettingsPage onSaved={vi.fn()} />);

    const saveButton = await screen.findByRole('button', { name: 'Kaydet' });
    expect(saveButton).toBeDisabled();
    expect(screen.getByText('Devam etmek için OpenRouter key gir.')).toBeInTheDocument();
    expect(api.getKeyStatus).not.toHaveBeenCalled();
  });

  it('başarılı kayıt sonrası key input\'ları temizlenir (write-only), başarı mesajı gösterilir, onSaved çağrılır', async () => {
    vi.mocked(api.getSettings).mockResolvedValue(settingsResponse());
    vi.mocked(api.getModels).mockResolvedValue(twoModels);
    vi.mocked(api.getKeyStatus).mockResolvedValue(freeKeyStatus);
    const savedResponse = settingsResponse({ keyPreview: '••••5678' });
    vi.mocked(api.saveSettings).mockResolvedValue(savedResponse);
    const onSaved = vi.fn();

    render(<SettingsPage onSaved={onSaved} />);
    const saveButton = await screen.findByRole('button', { name: 'Kaydet' });
    expect(saveButton).toBeEnabled();

    const openRouterInput = screen.getByPlaceholderText('Değiştirmek için yeni key gir (opsiyonel)');
    await userEvent.type(openRouterInput, 'yeni-key');
    await userEvent.click(saveButton);

    expect(await screen.findByText('Ayarlar kaydedildi.')).toBeInTheDocument();
    expect(openRouterInput).toHaveValue('');
    expect(onSaved).toHaveBeenCalledWith(savedResponse);
  });

  it('kayıt hatasında error-text gösterir, onSaved çağrılmaz', async () => {
    vi.mocked(api.getSettings).mockResolvedValue(settingsResponse());
    vi.mocked(api.getModels).mockResolvedValue(twoModels);
    vi.mocked(api.getKeyStatus).mockResolvedValue(freeKeyStatus);
    vi.mocked(api.saveSettings).mockRejectedValue(new Error('Ayarlar kaydedilemedi (HTTP 500)'));
    const onSaved = vi.fn();

    render(<SettingsPage onSaved={onSaved} />);
    await userEvent.click(await screen.findByRole('button', { name: 'Kaydet' }));

    expect(await screen.findByText('Ayarlar kaydedilemedi (HTTP 500)')).toBeInTheDocument();
    expect(onSaved).not.toHaveBeenCalled();
  });

  it('getKeyStatus yüklemede başarısız olursa sessizce yutulur, sayfa yine render olur', async () => {
    vi.mocked(api.getSettings).mockResolvedValue(settingsResponse());
    vi.mocked(api.getModels).mockResolvedValue(twoModels);
    vi.mocked(api.getKeyStatus).mockRejectedValue(new Error("OpenRouter'a ulaşılamadı"));

    render(<SettingsPage onSaved={vi.fn()} />);

    expect(await screen.findByText('OpenRouter API Key')).toBeInTheDocument();
    expect(screen.queryByText(/ulaşılamadı/)).not.toBeInTheDocument();
  });
});
