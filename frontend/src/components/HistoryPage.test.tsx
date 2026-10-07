import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { HistoryPage } from './HistoryPage';
import { buildCouncilResult } from '../test/fixtures';
import * as api from '../api';

vi.mock('../api', () => ({
  getConversations: vi.fn(),
  getConversationDetail: vi.fn(),
}));

function summary(overrides: Partial<api.ConversationSummary> = {}): api.ConversationSummary {
  return {
    id: 1,
    title: 'soru 1',
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
    turnCount: 1,
    ...overrides,
  };
}

describe('HistoryPage', () => {
  it('yükleme sonrası her konuşma için bir liste öğesi render eder', async () => {
    vi.mocked(api.getConversations).mockResolvedValue([summary()]);

    render(<HistoryPage onContinue={vi.fn()} />);

    expect(await screen.findByText('soru 1')).toBeInTheDocument();
  });

  it('liste boşsa "Henüz hiç soru sorulmamış." gösterir', async () => {
    vi.mocked(api.getConversations).mockResolvedValue([]);

    render(<HistoryPage onContinue={vi.fn()} />);

    expect(await screen.findByText('Henüz hiç soru sorulmamış.')).toBeInTheDocument();
  });

  it('bir öğeye tıklayınca ilgili thread detayı getirilip render edilir', async () => {
    vi.mocked(api.getConversations).mockResolvedValue([summary()]);
    vi.mocked(api.getConversationDetail).mockResolvedValue([buildCouncilResult({ presidentProvider: 'model-x' })]);

    render(<HistoryPage onContinue={vi.fn()} />);
    await userEvent.click(await screen.findByText('soru 1'));

    expect(await screen.findByText('Konsey Başkanı: model-x')).toBeInTheDocument();
    expect(api.getConversationDetail).toHaveBeenCalledWith(1);
  });

  it('detay getirme hatası error-text olarak gösterilir', async () => {
    vi.mocked(api.getConversations).mockResolvedValue([summary()]);
    vi.mocked(api.getConversationDetail).mockRejectedValue(new Error('Konuşma alınamadı'));

    render(<HistoryPage onContinue={vi.fn()} />);
    await userEvent.click(await screen.findByText('soru 1'));

    expect(await screen.findByText('Konuşma alınamadı')).toBeInTheDocument();
  });

  it('turlar yüklendikten sonra "devam et" tıklanınca onContinue(threadId, turns) ile çağrılır', async () => {
    const onContinue = vi.fn();
    vi.mocked(api.getConversations).mockResolvedValue([summary()]);
    const turns = [buildCouncilResult()];
    vi.mocked(api.getConversationDetail).mockResolvedValue(turns);

    render(<HistoryPage onContinue={onContinue} />);
    await userEvent.click(await screen.findByText('soru 1'));
    const continueButton = await screen.findByText('Bu konuşmaya devam et →');
    await userEvent.click(continueButton);

    expect(onContinue).toHaveBeenCalledWith(1, turns);
  });
});
