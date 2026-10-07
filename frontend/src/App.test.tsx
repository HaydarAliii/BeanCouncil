import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import App from './App';
import { buildCouncilResult } from './test/fixtures';
import * as api from './api';

vi.mock('./api', () => ({
  getSettings: vi.fn(),
  askCouncil: vi.fn(),
  getModels: vi.fn(),
  getKeyStatus: vi.fn(),
  saveSettings: vi.fn(),
  getConversations: vi.fn(),
  getConversationDetail: vi.fn(),
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

beforeEach(() => {
  // App'in kendi view-routing/turns state makinesini test ediyoruz. view settings/history'ye
  // geçerse çocuk component'lerin (SettingsPage/HistoryPage) kendi api çağrıları zararsız
  // varsayılanlarla tamamlansın diye (detayları kendi test dosyalarında zaten kapsandı).
  vi.mocked(api.getModels).mockResolvedValue([]);
  vi.mocked(api.getConversations).mockResolvedValue([]);
});

describe('App', () => {
  it('ayarlar hazır değilse (hasKey=false) mount\'ta Ayarlar görünümüne yönlendirir', async () => {
    vi.mocked(api.getSettings).mockResolvedValue(settingsResponse({ hasKey: false, keyPreview: null }));

    render(<App />);

    expect(await screen.findByText('OpenRouter API Key')).toBeInTheDocument();
    expect(screen.queryByText(/Sorunu birden fazla AI modeline sor/)).not.toBeInTheDocument();
  });

  it("ayarlar hazırsa Konsey görünümünde kalır, webSearchAvailable hasTavilyKey'i yansıtır", async () => {
    vi.mocked(api.getSettings).mockResolvedValue(settingsResponse({ hasTavilyKey: true }));

    render(<App />);

    expect(await screen.findByText(/Sorunu birden fazla AI modeline sor/)).toBeInTheDocument();
    expect(screen.getByRole('checkbox')).toBeInTheDocument(); // web'de ara seçeneği görünür
  });

  it('getSettings reddedilirse configured=false olur, Konsey görünümünde uyarı gösterilir', async () => {
    vi.mocked(api.getSettings).mockRejectedValue(new Error('ağ hatası'));

    render(<App />);

    expect(await screen.findByText(/Henüz yapılandırma tamamlanmadı/)).toBeInTheDocument();
  });

  it('prompt gönderimi askCouncil çağırır ve bir tur ekler', async () => {
    vi.mocked(api.getSettings).mockResolvedValue(settingsResponse());
    vi.mocked(api.askCouncil).mockResolvedValue(buildCouncilResult({ presidentProvider: 'model-x', threadId: 5 }));

    render(<App />);
    await screen.findByText(/Sorunu birden fazla AI modeline sor/);

    await userEvent.type(screen.getByPlaceholderText('Konseye sormak istediğin soruyu yaz...'), 'soru');
    await userEvent.click(screen.getByRole('button', { name: 'Konseye Sor' }));

    expect(await screen.findByText('Konsey Başkanı: model-x')).toBeInTheDocument();
    expect(api.askCouncil).toHaveBeenCalledWith('soru', null, false);
  });

  it('askCouncil hatası error-text gösterir, tur eklenmez', async () => {
    vi.mocked(api.getSettings).mockResolvedValue(settingsResponse());
    vi.mocked(api.askCouncil).mockRejectedValue(new Error('Konsey isteği başarısız oldu (HTTP 500)'));

    render(<App />);
    await screen.findByText(/Sorunu birden fazla AI modeline sor/);

    await userEvent.type(screen.getByPlaceholderText('Konseye sormak istediğin soruyu yaz...'), 'soru');
    await userEvent.click(screen.getByRole('button', { name: 'Konseye Sor' }));

    expect(await screen.findByText('Konsey isteği başarısız oldu (HTTP 500)')).toBeInTheDocument();
    expect(screen.queryByText(/Konsey Başkanı/)).not.toBeInTheDocument();
  });

  it('"+ Yeni konuşma" turları ve threadId\'yi sıfırlar', async () => {
    vi.mocked(api.getSettings).mockResolvedValue(settingsResponse());
    vi.mocked(api.askCouncil).mockResolvedValue(buildCouncilResult({ presidentProvider: 'model-x', threadId: 5 }));

    render(<App />);
    await screen.findByText(/Sorunu birden fazla AI modeline sor/);
    await userEvent.type(screen.getByPlaceholderText('Konseye sormak istediğin soruyu yaz...'), 'soru');
    await userEvent.click(screen.getByRole('button', { name: 'Konseye Sor' }));
    await screen.findByText('Konsey Başkanı: model-x');

    await userEvent.click(screen.getByRole('button', { name: '+ Yeni konuşma' }));

    expect(screen.queryByText('Konsey Başkanı: model-x')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '+ Yeni konuşma' })).not.toBeInTheDocument();
  });

  it('ikinci gönderim önceki turdan dönen threadId\'yi askCouncil\'e geçirir (thread sürekliliği)', async () => {
    vi.mocked(api.getSettings).mockResolvedValue(settingsResponse());
    vi.mocked(api.askCouncil).mockResolvedValue(buildCouncilResult({ presidentProvider: 'model-x', threadId: 5 }));

    render(<App />);
    await screen.findByText(/Sorunu birden fazla AI modeline sor/);

    const textarea = () => screen.getByPlaceholderText(/sormak istediğin soruyu yaz|Takip sorunu yaz/);
    await userEvent.type(textarea(), 'ilk soru');
    await userEvent.click(screen.getByRole('button', { name: 'Konseye Sor' }));
    await screen.findByText('Konsey Başkanı: model-x');

    await userEvent.type(textarea(), 'takip sorusu');
    await userEvent.click(screen.getByRole('button', { name: 'Konseye Sor' }));

    expect(api.askCouncil).toHaveBeenLastCalledWith('takip sorusu', 5, false);
  });
});
