import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { KeyInputPanel } from './KeyInputPanel';

const baseProps = {
  title: 'OpenRouter API Key',
  description: 'açıklama',
  value: '',
  onChange: vi.fn(),
  emptyPlaceholder: 'sk-or-v1-...',
};

describe('KeyInputPanel', () => {
  it('hasKey=true iken kayıtlı key önizlemesini gösterir', () => {
    render(<KeyInputPanel {...baseProps} hasKey keyPreview="••••1234" />);

    expect(screen.getByText('••••1234')).toBeInTheDocument();
  });

  it('hasKey=false iken önizleme satırı hiç görünmez, emptyPlaceholder kullanılır', () => {
    render(<KeyInputPanel {...baseProps} hasKey={false} keyPreview={null} />);

    expect(screen.queryByText(/Kayıtlı key/)).not.toBeInTheDocument();
    expect(screen.getByPlaceholderText('sk-or-v1-...')).toBeInTheDocument();
  });

  it('hasKey=true iken placeholder "Değiştirmek için..." metnine döner', () => {
    render(<KeyInputPanel {...baseProps} hasKey keyPreview="••••1234" />);

    expect(screen.getByPlaceholderText('Değiştirmek için yeni key gir (opsiyonel)')).toBeInTheDocument();
  });

  it('yazı yazıldığında onChange tetiklenir', async () => {
    const onChange = vi.fn();
    render(<KeyInputPanel {...baseProps} hasKey={false} keyPreview={null} onChange={onChange} />);

    // type="password" input'u getByRole('textbox') ile bulunamaz — placeholder üzerinden eriş.
    const input = screen.getByPlaceholderText('sk-or-v1-...');
    await userEvent.type(input, 'x');

    expect(onChange).toHaveBeenCalledWith('x');
  });
});
