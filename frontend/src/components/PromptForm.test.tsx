import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { PromptForm } from './PromptForm';

describe('PromptForm', () => {
  it('calls onSubmit with the trimmed prompt + webSearch=false on type and submit', async () => {
    const onSubmit = vi.fn();
    render(<PromptForm loading={false} onSubmit={onSubmit} />);

    await userEvent.type(screen.getByPlaceholderText('Write the question you want to ask the council...'), '  question  ');
    await userEvent.click(screen.getByRole('button', { name: 'Ask Council' }));

    expect(onSubmit).toHaveBeenCalledWith('question', false);
  });

  it('clears the textarea after submitting', async () => {
    render(<PromptForm loading={false} onSubmit={vi.fn()} />);
    const textarea = screen.getByPlaceholderText('Write the question you want to ask the council...');

    await userEvent.type(textarea, 'question');
    await userEvent.click(screen.getByRole('button', { name: 'Ask Council' }));

    expect(textarea).toHaveValue('');
  });

  it('disables the button when the prompt is empty and when loading is true', async () => {
    const { rerender } = render(<PromptForm loading={false} onSubmit={vi.fn()} />);
    expect(screen.getByRole('button', { name: 'Ask Council' })).toBeDisabled();

    await userEvent.type(screen.getByPlaceholderText('Write the question you want to ask the council...'), 'question');
    expect(screen.getByRole('button', { name: 'Ask Council' })).toBeEnabled();

    rerender(<PromptForm loading onSubmit={vi.fn()} />);
    expect(screen.getByRole('button', { name: 'Council is deliberating…' })).toBeDisabled();
  });

  it('never shows the checkbox when webSearchAvailable is not set', () => {
    render(<PromptForm loading={false} onSubmit={vi.fn()} />);

    expect(screen.queryByRole('checkbox')).not.toBeInTheDocument();
  });

  it('shows the checkbox when webSearchAvailable is true, calls onSubmit with webSearch=true when checked', async () => {
    const onSubmit = vi.fn();
    render(<PromptForm loading={false} onSubmit={onSubmit} webSearchAvailable />);

    await userEvent.click(screen.getByRole('checkbox'));
    await userEvent.type(screen.getByPlaceholderText('Write the question you want to ask the council...'), 'question');
    await userEvent.click(screen.getByRole('button', { name: 'Ask Council' }));

    expect(onSubmit).toHaveBeenCalledWith('question', true);
  });
});
