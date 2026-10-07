import { describe, expect, it } from 'vitest';
import {
  askCouncil,
  getConversationDetail,
  getConversations,
  getKeyStatus,
  getModels,
  getSettings,
  saveSettings,
} from './api';
import { mockFetchOnce } from './test/fetchMock';

describe('askCouncil', () => {
  it('POST /api/council/ask ile prompt/threadId/webSearch gönderir ve yanıtı döner', async () => {
    const fetchMock = mockFetchOnce({ body: { finalAnswer: 'cevap' } });

    const result = await askCouncil('soru', 7, true);

    expect(result).toEqual({ finalAnswer: 'cevap' });
    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe('/api/council/ask');
    expect(init.method).toBe('POST');
    expect(init.headers).toEqual({ 'Content-Type': 'application/json' });
    expect(JSON.parse(init.body)).toEqual({ prompt: 'soru', threadId: 7, webSearch: true });
  });

  it('threadId/webSearch verilmezse varsayılan olarak null/false gönderir', async () => {
    const fetchMock = mockFetchOnce({ body: {} });

    await askCouncil('soru');

    const [, init] = fetchMock.mock.calls[0];
    expect(JSON.parse(init.body)).toEqual({ prompt: 'soru', threadId: null, webSearch: false });
  });
});

describe('getSettings', () => {
  it('GET /api/settings çağırır ve yanıtı döner', async () => {
    const fetchMock = mockFetchOnce({ body: { hasKey: true } });

    const result = await getSettings();

    expect(result).toEqual({ hasKey: true });
    expect(fetchMock.mock.calls[0][0]).toBe('/api/settings');
  });
});

describe('saveSettings', () => {
  it('PUT /api/settings ile request gövdesini olduğu gibi gönderir', async () => {
    const fetchMock = mockFetchOnce({ body: { hasKey: true } });
    const request = { selectedModelIds: ['a', 'b'], presidentModelId: 'a' };

    await saveSettings(request);

    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe('/api/settings');
    expect(init.method).toBe('PUT');
    expect(JSON.parse(init.body)).toEqual(request);
  });
});

describe('getModels', () => {
  it('{models:[...]} sarmalayıcısını açıp sadece diziyi döner', async () => {
    mockFetchOnce({ body: { models: [{ id: 'a' }, { id: 'b' }] } });

    const result = await getModels();

    expect(result).toEqual([{ id: 'a' }, { id: 'b' }]);
  });
});

describe('getKeyStatus', () => {
  it('GET /api/models/key çağırır', async () => {
    const fetchMock = mockFetchOnce({ body: { label: 'L' } });

    const result = await getKeyStatus();

    expect(result).toEqual({ label: 'L' });
    expect(fetchMock.mock.calls[0][0]).toBe('/api/models/key');
  });
});

describe('getConversations', () => {
  it('GET /api/conversations çağırır', async () => {
    const fetchMock = mockFetchOnce({ body: [] });

    await getConversations();

    expect(fetchMock.mock.calls[0][0]).toBe('/api/conversations');
  });
});

describe('getConversationDetail', () => {
  it('threadId ile doğru URL\'i oluşturur', async () => {
    const fetchMock = mockFetchOnce({ body: [] });

    await getConversationDetail(42);

    expect(fetchMock.mock.calls[0][0]).toBe('/api/conversations/42');
  });
});

describe('handle() hata gövdesi parse mantığı (tüm fonksiyonlar için ortak)', () => {
  it('JSON gövdede {error} varsa o mesajı fırlatır', async () => {
    mockFetchOnce({ status: 428, body: { error: 'Ayarlar henüz yapılandırılmamış.' } });

    await expect(getSettings()).rejects.toThrow('Ayarlar henüz yapılandırılmamış.');
  });

  it('JSON gövde var ama {error} alanı yoksa fallback + HTTP status mesajı fırlatır', async () => {
    mockFetchOnce({ status: 500, body: { somethingElse: true } });

    await expect(getSettings()).rejects.toThrow('Ayarlar alınamadı (HTTP 500)');
  });

  it('gövde JSON değilse parse hatasını yutup fallback + HTTP status mesajı fırlatır', async () => {
    mockFetchOnce({ status: 503, jsonThrows: true });

    await expect(getSettings()).rejects.toThrow('Ayarlar alınamadı (HTTP 503)');
  });

  it('her fonksiyon kendi fallback mesajını kullanır (ör. askCouncil)', async () => {
    mockFetchOnce({ status: 500, jsonThrows: true });

    await expect(askCouncil('soru')).rejects.toThrow('Konsey isteği başarısız oldu (HTTP 500)');
  });

  it('response.ok ise hata fırlatmadan doğrudan json() sonucunu döner', async () => {
    mockFetchOnce({ status: 200, body: { hasKey: false } });

    await expect(getSettings()).resolves.toEqual({ hasKey: false });
  });
});
