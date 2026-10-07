import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { PromptForm } from './PromptForm';

describe('PromptForm', () => {
  it('yazıp gönderince onSubmit trim edilmiş prompt + webSearch=false ile çağrılır', async () => {
    const onSubmit = vi.fn();
    render(<PromptForm loading={false} onSubmit={onSubmit} />);

    await userEvent.type(screen.getByPlaceholderText('Konseye sormak istediğin soruyu yaz...'), '  soru  ');
    await userEvent.click(screen.getByRole('button', { name: 'Konseye Sor' }));

    expect(onSubmit).toHaveBeenCalledWith('soru', false);
  });

  it('gönderimden sonra textarea temizlenir', async () => {
    render(<PromptForm loading={false} onSubmit={vi.fn()} />);
    const textarea = screen.getByPlaceholderText('Konseye sormak istediğin soruyu yaz...');

    await userEvent.type(textarea, 'soru');
    await userEvent.click(screen.getByRole('button', { name: 'Konseye Sor' }));

    expect(textarea).toHaveValue('');
  });

  it('prompt boşken ve loading true iken buton disabled olur', async () => {
    const { rerender } = render(<PromptForm loading={false} onSubmit={vi.fn()} />);
    expect(screen.getByRole('button', { name: 'Konseye Sor' })).toBeDisabled();

    await userEvent.type(screen.getByPlaceholderText('Konseye sormak istediğin soruyu yaz...'), 'soru');
    expect(screen.getByRole('button', { name: 'Konseye Sor' })).toBeEnabled();

    rerender(<PromptForm loading onSubmit={vi.fn()} />);
    expect(screen.getByRole('button', { name: 'Konsey görüşüyor…' })).toBeDisabled();
  });

  it('webSearchAvailable olmadığında checkbox hiç görünmez', () => {
    render(<PromptForm loading={false} onSubmit={vi.fn()} />);

    expect(screen.queryByRole('checkbox')).not.toBeInTheDocument();
  });

  it('webSearchAvailable true iken checkbox görünür ve işaretlenince onSubmit webSearch=true ile çağrılır', async () => {
    const onSubmit = vi.fn();
    render(<PromptForm loading={false} onSubmit={onSubmit} webSearchAvailable />);

    await userEvent.click(screen.getByRole('checkbox'));
    await userEvent.type(screen.getByPlaceholderText('Konseye sormak istediğin soruyu yaz...'), 'soru');
    await userEvent.click(screen.getByRole('button', { name: 'Konseye Sor' }));

    expect(onSubmit).toHaveBeenCalledWith('soru', true);
  });
});
