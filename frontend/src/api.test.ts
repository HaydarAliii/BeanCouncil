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
  it('sends prompt/threadId/webSearch via POST /api/council/ask and returns the response', async () => {
    const fetchMock = mockFetchOnce({ body: { finalAnswer: 'answer' } });

    const result = await askCouncil('question', 7, true);

    expect(result).toEqual({ finalAnswer: 'answer' });
    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe('/api/council/ask');
    expect(init.method).toBe('POST');
    expect(init.headers).toEqual({ 'Content-Type': 'application/json' });
    expect(JSON.parse(init.body)).toEqual({ prompt: 'question', threadId: 7, webSearch: true });
  });

  it('defaults to null/false when threadId/webSearch are not given', async () => {
    const fetchMock = mockFetchOnce({ body: {} });

    await askCouncil('question');

    const [, init] = fetchMock.mock.calls[0];
    expect(JSON.parse(init.body)).toEqual({ prompt: 'question', threadId: null, webSearch: false });
  });
});

describe('getSettings', () => {
  it('calls GET /api/settings and returns the response', async () => {
    const fetchMock = mockFetchOnce({ body: { hasKey: true } });

    const result = await getSettings();

    expect(result).toEqual({ hasKey: true });
    expect(fetchMock.mock.calls[0][0]).toBe('/api/settings');
  });
});

describe('saveSettings', () => {
  it('sends the request body as-is via PUT /api/settings', async () => {
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
  it('unwraps the {models:[...]} wrapper and returns just the array', async () => {
    mockFetchOnce({ body: { models: [{ id: 'a' }, { id: 'b' }] } });

    const result = await getModels();

    expect(result).toEqual([{ id: 'a' }, { id: 'b' }]);
  });
});

describe('getKeyStatus', () => {
  it('calls GET /api/models/key', async () => {
    const fetchMock = mockFetchOnce({ body: { label: 'L' } });

    const result = await getKeyStatus();

    expect(result).toEqual({ label: 'L' });
    expect(fetchMock.mock.calls[0][0]).toBe('/api/models/key');
  });
});

describe('getConversations', () => {
  it('calls GET /api/conversations', async () => {
    const fetchMock = mockFetchOnce({ body: [] });

    await getConversations();

    expect(fetchMock.mock.calls[0][0]).toBe('/api/conversations');
  });
});

describe('getConversationDetail', () => {
  it('builds the correct URL from threadId', async () => {
    const fetchMock = mockFetchOnce({ body: [] });

    await getConversationDetail(42);

    expect(fetchMock.mock.calls[0][0]).toBe('/api/conversations/42');
  });
});

describe('handle() error body parsing logic (shared by all functions)', () => {
  it('throws that message when the JSON body has {error}', async () => {
    mockFetchOnce({ status: 428, body: { error: 'Settings have not been configured yet.' } });

    await expect(getSettings()).rejects.toThrow('Settings have not been configured yet.');
  });

  it('throws fallback + HTTP status message when the JSON body has no {error} field', async () => {
    mockFetchOnce({ status: 500, body: { somethingElse: true } });

    await expect(getSettings()).rejects.toThrow('Could not load settings (HTTP 500)');
  });

  it('swallows the parse error and throws fallback + HTTP status message when the body is not JSON', async () => {
    mockFetchOnce({ status: 503, jsonThrows: true });

    await expect(getSettings()).rejects.toThrow('Could not load settings (HTTP 503)');
  });

  it('each function uses its own fallback message (e.g. askCouncil)', async () => {
    mockFetchOnce({ status: 500, jsonThrows: true });

    await expect(askCouncil('question')).rejects.toThrow('Council request failed (HTTP 500)');
  });

  it('returns the json() result directly without throwing when response.ok is true', async () => {
    mockFetchOnce({ status: 200, body: { hasKey: false } });

    await expect(getSettings()).resolves.toEqual({ hasKey: false });
  });
});
