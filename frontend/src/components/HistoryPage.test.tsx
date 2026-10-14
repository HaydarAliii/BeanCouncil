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
    title: 'question 1',
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
    turnCount: 1,
    ...overrides,
  };
}

describe('HistoryPage', () => {
  it('renders one list item per conversation after loading', async () => {
    vi.mocked(api.getConversations).mockResolvedValue([summary()]);

    render(<HistoryPage onContinue={vi.fn()} />);

    expect(await screen.findByText('question 1')).toBeInTheDocument();
  });

  it('shows "No questions asked yet." when the list is empty', async () => {
    vi.mocked(api.getConversations).mockResolvedValue([]);

    render(<HistoryPage onContinue={vi.fn()} />);

    expect(await screen.findByText('No questions asked yet.')).toBeInTheDocument();
  });

  it('fetches and renders the matching thread detail when an item is clicked', async () => {
    vi.mocked(api.getConversations).mockResolvedValue([summary()]);
    vi.mocked(api.getConversationDetail).mockResolvedValue([buildCouncilResult({ presidentProvider: 'model-x' })]);

    render(<HistoryPage onContinue={vi.fn()} />);
    await userEvent.click(await screen.findByText('question 1'));

    expect(await screen.findByText('Council President: model-x')).toBeInTheDocument();
    expect(api.getConversationDetail).toHaveBeenCalledWith(1);
  });

  it('shows detail fetch errors as error-text', async () => {
    vi.mocked(api.getConversations).mockResolvedValue([summary()]);
    vi.mocked(api.getConversationDetail).mockRejectedValue(new Error('Could not load conversation'));

    render(<HistoryPage onContinue={vi.fn()} />);
    await userEvent.click(await screen.findByText('question 1'));

    expect(await screen.findByText('Could not load conversation')).toBeInTheDocument();
  });

  it('calls onContinue(threadId, turns) when "continue" is clicked after turns load', async () => {
    const onContinue = vi.fn();
    vi.mocked(api.getConversations).mockResolvedValue([summary()]);
    const turns = [buildCouncilResult()];
    vi.mocked(api.getConversationDetail).mockResolvedValue(turns);

    render(<HistoryPage onContinue={onContinue} />);
    await userEvent.click(await screen.findByText('question 1'));
    const continueButton = await screen.findByText('Continue this conversation →');
    await userEvent.click(continueButton);

    expect(onContinue).toHaveBeenCalledWith(1, turns);
  });
});
