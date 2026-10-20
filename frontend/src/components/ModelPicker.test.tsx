import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { ModelPicker } from './ModelPicker';
import type { ModelInfo } from '../api';

const models: ModelInfo[] = [
  {
    id: 'anthropic/claude-sonnet-5',
    name: 'Claude Sonnet 5',
    contextLength: 1000000,
    promptPrice: '0.000002',
    completionPrice: '0.00001',
    isFree: false,
  },
  {
    id: 'qwen/qwen3.8-27b:free',
    name: 'Qwen 3.8 27B',
    contextLength: 262144,
    promptPrice: '0',
    completionPrice: '0',
    isFree: true,
  },
];

describe('ModelPicker', () => {
  it('renders one row per model, the filter narrows case-insensitively on id/name', async () => {
    render(
      <ModelPicker models={models} keyStatus={null} selected={[]} president={null} onToggle={vi.fn()} onSelectPresident={vi.fn()} />,
    );

    expect(screen.getByText('Claude Sonnet 5')).toBeInTheDocument();
    expect(screen.getByText('Qwen 3.8 27B')).toBeInTheDocument();

    await userEvent.type(screen.getByPlaceholderText(/Search models/), 'QWEN');

    expect(screen.queryByText('Claude Sonnet 5')).not.toBeInTheDocument();
    expect(screen.getByText('Qwen 3.8 27B')).toBeInTheDocument();
  });

  it('shows "No matching models found." when there is no match', async () => {
    render(
      <ModelPicker models={models} keyStatus={null} selected={[]} president={null} onToggle={vi.fn()} onSelectPresident={vi.fn()} />,
    );

    await userEvent.type(screen.getByPlaceholderText(/Search models/), 'zzz-none');

    expect(screen.getByText('No matching models found.')).toBeInTheDocument();
  });

  it('calls onToggle with the correct model id when a checkbox is checked', async () => {
    const onToggle = vi.fn();
    render(
      <ModelPicker models={models} keyStatus={null} selected={[]} president={null} onToggle={onToggle} onSelectPresident={vi.fn()} />,
    );

    const checkboxes = screen.getAllByRole('checkbox');
    await userEvent.click(checkboxes[1]);

    expect(onToggle).toHaveBeenCalledWith('qwen/qwen3.8-27b:free');
  });

  it('the president radio is only enabled for selected models, calls onSelectPresident when chosen', async () => {
    const onSelectPresident = vi.fn();
    render(
      <ModelPicker
        models={models}
        keyStatus={null}
        selected={['qwen/qwen3.8-27b:free']}
        president={null}
        onToggle={vi.fn()}
        onSelectPresident={onSelectPresident}
      />,
    );

    const radios = screen.getAllByRole('radio');
    expect(radios[0]).toBeDisabled(); // claude is not selected
    expect(radios[1]).toBeEnabled(); // qwen is selected

    await userEvent.click(radios[1]);
    expect(onSelectPresident).toHaveBeenCalledWith('qwen/qwen3.8-27b:free');
  });

  it('computes remaining credit when keyStatus has limit/usage, never renders credit-status without keyStatus', () => {
    const { rerender, container } = render(
      <ModelPicker models={models} keyStatus={null} selected={[]} president={null} onToggle={vi.fn()} onSelectPresident={vi.fn()} />,
    );
    expect(container.querySelector('.credit-status')).not.toBeInTheDocument();

    rerender(
      <ModelPicker
        models={models}
        keyStatus={{ label: 'L', limit: 10, usage: 3, isFreeTier: false, limitRemaining: 7 }}
        selected={[]}
        president={null}
        onToggle={vi.fn()}
        onSelectPresident={vi.fn()}
      />,
    );

    expect(screen.getByText(/\$7\.00 \/ \$10\.00 left/)).toBeInTheDocument();
    // "Paid account" and "— credit: ..." are separate text nodes within the same <p> — check
    // the full text content of .credit-status instead of an exact text match.
    expect(container.querySelector('.credit-status')).toHaveTextContent('Paid account');
  });

  it('shows the correct free/paid badge text based on model.isFree', () => {
    render(
      <ModelPicker models={models} keyStatus={null} selected={[]} president={null} onToggle={vi.fn()} onSelectPresident={vi.fn()} />,
    );

    expect(screen.getByText('Paid')).toBeInTheDocument();
    expect(screen.getByText('Free')).toBeInTheDocument();
  });

  it('shows a "Local" badge for ollama/-prefixed models instead of Free/Paid', () => {
    const localModel: ModelInfo = {
      id: 'ollama/llama3.1',
      name: 'llama3.1',
      contextLength: null,
      promptPrice: '0',
      completionPrice: '0',
      isFree: true,
    };
    render(
      <ModelPicker
        models={[...models, localModel]}
        keyStatus={null}
        selected={[]}
        president={null}
        onToggle={vi.fn()}
        onSelectPresident={vi.fn()}
      />,
    );

    expect(screen.getByText('Local')).toBeInTheDocument();
    // The free-badge count stays at just the Qwen model — the local model doesn't double up as "Free".
    expect(screen.getAllByText('Free')).toHaveLength(1);
  });
});
