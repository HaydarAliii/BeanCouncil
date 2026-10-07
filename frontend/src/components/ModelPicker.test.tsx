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
  it('her model için bir satır render eder, filtre id/name üzerinde case-insensitive daraltır', async () => {
    render(
      <ModelPicker models={models} keyStatus={null} selected={[]} president={null} onToggle={vi.fn()} onSelectPresident={vi.fn()} />,
    );

    expect(screen.getByText('Claude Sonnet 5')).toBeInTheDocument();
    expect(screen.getByText('Qwen 3.8 27B')).toBeInTheDocument();

    await userEvent.type(screen.getByPlaceholderText(/Model ara/), 'QWEN');

    expect(screen.queryByText('Claude Sonnet 5')).not.toBeInTheDocument();
    expect(screen.getByText('Qwen 3.8 27B')).toBeInTheDocument();
  });

  it('eşleşme yoksa "Eşleşen model bulunamadı." gösterir', async () => {
    render(
      <ModelPicker models={models} keyStatus={null} selected={[]} president={null} onToggle={vi.fn()} onSelectPresident={vi.fn()} />,
    );

    await userEvent.type(screen.getByPlaceholderText(/Model ara/), 'zzz-yok');

    expect(screen.getByText('Eşleşen model bulunamadı.')).toBeInTheDocument();
  });

  it('checkbox işaretlenince onToggle doğru model id ile çağrılır', async () => {
    const onToggle = vi.fn();
    render(
      <ModelPicker models={models} keyStatus={null} selected={[]} president={null} onToggle={onToggle} onSelectPresident={vi.fn()} />,
    );

    const checkboxes = screen.getAllByRole('checkbox');
    await userEvent.click(checkboxes[1]);

    expect(onToggle).toHaveBeenCalledWith('qwen/qwen3.8-27b:free');
  });

  it('başkan radio sadece seçili modellerde aktif olur, seçilince onSelectPresident çağrılır', async () => {
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
    expect(radios[0]).toBeDisabled(); // claude seçili değil
    expect(radios[1]).toBeEnabled(); // qwen seçili

    await userEvent.click(radios[1]);
    expect(onSelectPresident).toHaveBeenCalledWith('qwen/qwen3.8-27b:free');
  });

  it('keyStatus limit/usage varsa kalan krediyi hesaplar, keyStatus yoksa credit-status hiç render olmaz', () => {
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

    expect(screen.getByText(/7\.00 \/ 10\.00 \$ kaldı/)).toBeInTheDocument();
    // "Ücretli hesap" ve "— kredi: ..." aynı <p> içinde ayrı text node'lar — tam metin eşleşmesi
    // yerine .credit-status'un tüm metnini kontrol ediyoruz.
    expect(container.querySelector('.credit-status')).toHaveTextContent('Ücretli hesap');
  });

  it("ücretsiz/ücretli rozeti model.isFree'ye göre doğru metni gösterir", () => {
    render(
      <ModelPicker models={models} keyStatus={null} selected={[]} president={null} onToggle={vi.fn()} onSelectPresident={vi.fn()} />,
    );

    expect(screen.getByText('Ücretli')).toBeInTheDocument();
    expect(screen.getByText('Ücretsiz')).toBeInTheDocument();
  });
});
