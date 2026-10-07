import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { ProcessDetails } from './ProcessDetails';
import { buildCouncilResult, buildLlmResponse } from '../test/fixtures';

describe('ProcessDetails', () => {
  it('webSearchResults boş dizi iken web araması bölümü hiç render olmaz (regresyon testi)', () => {
    // Bu oturumda gerçek bir bug buydu: eski transcript'lerde webSearchResults hiç yoktu,
    // backend bunu artık [] olarak normalize ediyor (CouncilResult compact constructor).
    // Frontend'in bu durumda bölümü göstermemesi gerekiyor, çökmemesi değil sadece.
    const result = buildCouncilResult({ webSearchResults: [] });

    render(<ProcessDetails result={result} />);

    expect(screen.queryByText('🔍 Web Araştırması Sonuçları')).not.toBeInTheDocument();
  });

  it('webSearchResults doluyken başlık ve her sonuç için bir kart render olur', () => {
    const result = buildCouncilResult({
      webSearchResults: [
        { title: 'Kaynak 1', url: 'https://example.com/1', content: 'özet 1' },
        { title: 'Kaynak 2', url: 'https://example.com/2', content: 'özet 2' },
      ],
    });

    render(<ProcessDetails result={result} />);

    expect(screen.getByText('🔍 Web Araştırması Sonuçları')).toBeInTheDocument();
    const links = screen.getAllByRole('link');
    expect(links).toHaveLength(2);
    expect(links[0]).toHaveAttribute('href', 'https://example.com/1');
    expect(screen.getByText('Kaynak 1')).toBeInTheDocument();
    expect(screen.getByText('Kaynak 2')).toBeInTheDocument();
  });

  it('başarılı ilk görüş markdown içeriğini, başarısız olan hata mesajını gösterir', () => {
    const result = buildCouncilResult({
      firstOpinions: [
        buildLlmResponse({ providerName: 'model-a', success: true, content: 'başarılı görüş' }),
        buildLlmResponse({
          providerName: 'model-b',
          success: false,
          content: null,
          errorMessage: 'zaman aşımı',
        }),
      ],
    });

    render(<ProcessDetails result={result} />);

    expect(screen.getByText('başarılı görüş')).toBeInTheDocument();
    expect(screen.getByText('zaman aşımı')).toBeInTheDocument();
  });

  it('reviews listesindeki her eleman için bir kart render olur', () => {
    const result = buildCouncilResult({
      reviews: [
        buildLlmResponse({ providerName: 'model-a', content: 'review A' }),
        buildLlmResponse({ providerName: 'model-b', content: 'review B' }),
      ],
    });

    render(<ProcessDetails result={result} />);

    expect(screen.getByText('review A')).toBeInTheDocument();
    expect(screen.getByText('review B')).toBeInTheDocument();
  });
});
