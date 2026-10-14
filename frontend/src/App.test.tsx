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
  // We're testing App's own view-routing/turns state machine. If the view switches to
  // settings/history, let the child components' (SettingsPage/HistoryPage) own api calls
  // resolve with harmless defaults (already covered in detail in their own test files).
  vi.mocked(api.getModels).mockResolvedValue([]);
  vi.mocked(api.getConversations).mockResolvedValue([]);
});

describe('App', () => {
  it('redirects to the Settings view on mount if settings are not ready (hasKey=false)', async () => {
    vi.mocked(api.getSettings).mockResolvedValue(settingsResponse({ hasKey: false, keyPreview: null }));

    render(<App />);

    expect(await screen.findByText('OpenRouter API Key')).toBeInTheDocument();
    expect(screen.queryByText(/Ask one question to multiple AI models/)).not.toBeInTheDocument();
  });

  it('stays on the Council view when settings are ready, webSearchAvailable reflects hasTavilyKey', async () => {
    vi.mocked(api.getSettings).mockResolvedValue(settingsResponse({ hasTavilyKey: true }));

    render(<App />);

    expect(await screen.findByText(/Ask one question to multiple AI models/)).toBeInTheDocument();
    expect(screen.getByRole('checkbox')).toBeInTheDocument(); // the "search the web" option is visible
  });

  it('sets configured=false if getSettings is rejected, shows a warning on the Council view', async () => {
    vi.mocked(api.getSettings).mockRejectedValue(new Error('network error'));

    render(<App />);

    expect(await screen.findByText(/Setup isn't finished yet/)).toBeInTheDocument();
  });

  it('submitting a prompt calls askCouncil and adds a turn', async () => {
    vi.mocked(api.getSettings).mockResolvedValue(settingsResponse());
    vi.mocked(api.askCouncil).mockResolvedValue(buildCouncilResult({ presidentProvider: 'model-x', threadId: 5 }));

    render(<App />);
    await screen.findByText(/Ask one question to multiple AI models/);

    await userEvent.type(screen.getByPlaceholderText('Write the question you want to ask the council...'), 'question');
    await userEvent.click(screen.getByRole('button', { name: 'Ask Council' }));

    expect(await screen.findByText('Council President: model-x')).toBeInTheDocument();
    expect(api.askCouncil).toHaveBeenCalledWith('question', null, false);
  });

  it('shows error-text on askCouncil failure, no turn is added', async () => {
    vi.mocked(api.getSettings).mockResolvedValue(settingsResponse());
    vi.mocked(api.askCouncil).mockRejectedValue(new Error('Council request failed (HTTP 500)'));

    render(<App />);
    await screen.findByText(/Ask one question to multiple AI models/);

    await userEvent.type(screen.getByPlaceholderText('Write the question you want to ask the council...'), 'question');
    await userEvent.click(screen.getByRole('button', { name: 'Ask Council' }));

    expect(await screen.findByText('Council request failed (HTTP 500)')).toBeInTheDocument();
    expect(screen.queryByText(/Council President/)).not.toBeInTheDocument();
  });

  it('"+ New conversation" resets turns and threadId', async () => {
    vi.mocked(api.getSettings).mockResolvedValue(settingsResponse());
    vi.mocked(api.askCouncil).mockResolvedValue(buildCouncilResult({ presidentProvider: 'model-x', threadId: 5 }));

    render(<App />);
    await screen.findByText(/Ask one question to multiple AI models/);
    await userEvent.type(screen.getByPlaceholderText('Write the question you want to ask the council...'), 'question');
    await userEvent.click(screen.getByRole('button', { name: 'Ask Council' }));
    await screen.findByText('Council President: model-x');

    await userEvent.click(screen.getByRole('button', { name: '+ New conversation' }));

    expect(screen.queryByText('Council President: model-x')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '+ New conversation' })).not.toBeInTheDocument();
  });

  it('a second submission passes the threadId returned from the previous turn to askCouncil (thread continuity)', async () => {
    vi.mocked(api.getSettings).mockResolvedValue(settingsResponse());
    vi.mocked(api.askCouncil).mockResolvedValue(buildCouncilResult({ presidentProvider: 'model-x', threadId: 5 }));

    render(<App />);
    await screen.findByText(/Ask one question to multiple AI models/);

    const textarea = () => screen.getByPlaceholderText(/ask the council|Write a follow-up question/);
    await userEvent.type(textarea(), 'first question');
    await userEvent.click(screen.getByRole('button', { name: 'Ask Council' }));
    await screen.findByText('Council President: model-x');

    await userEvent.type(textarea(), 'follow-up question');
    await userEvent.click(screen.getByRole('button', { name: 'Ask Council' }));

    expect(api.askCouncil).toHaveBeenLastCalledWith('follow-up question', 5, false);
  });
});
