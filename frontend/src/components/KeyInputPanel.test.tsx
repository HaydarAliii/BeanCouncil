import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { KeyInputPanel } from './KeyInputPanel';

const baseProps = {
  title: 'OpenRouter API Key',
  description: 'description',
  value: '',
  onChange: vi.fn(),
  emptyPlaceholder: 'sk-or-v1-...',
};

describe('KeyInputPanel', () => {
  it('shows the saved key preview when hasKey=true', () => {
    render(<KeyInputPanel {...baseProps} hasKey keyPreview="••••1234" />);

    expect(screen.getByText('••••1234')).toBeInTheDocument();
  });

  it('hides the preview line entirely when hasKey=false, uses emptyPlaceholder', () => {
    render(<KeyInputPanel {...baseProps} hasKey={false} keyPreview={null} />);

    expect(screen.queryByText(/Saved key/)).not.toBeInTheDocument();
    expect(screen.getByPlaceholderText('sk-or-v1-...')).toBeInTheDocument();
  });

  it('switches the placeholder to "Enter a new key..." when hasKey=true', () => {
    render(<KeyInputPanel {...baseProps} hasKey keyPreview="••••1234" />);

    expect(screen.getByPlaceholderText('Enter a new key to change it (optional)')).toBeInTheDocument();
  });

  it('triggers onChange when typing', async () => {
    const onChange = vi.fn();
    render(<KeyInputPanel {...baseProps} hasKey={false} keyPreview={null} onChange={onChange} />);

    // type="password" input can't be found via getByRole('textbox') — use the placeholder instead.
    const input = screen.getByPlaceholderText('sk-or-v1-...');
    await userEvent.type(input, 'x');

    expect(onChange).toHaveBeenCalledWith('x');
  });
});
