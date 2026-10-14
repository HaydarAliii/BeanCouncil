import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { ProcessDetails } from './ProcessDetails';
import { buildCouncilResult, buildLlmResponse } from '../test/fixtures';

describe('ProcessDetails', () => {
  it('does not render the web search section at all when webSearchResults is empty (regression test)', () => {
    // This was a real bug in this session: old transcripts never had webSearchResults at all,
    // the backend now normalizes it to [] (CouncilResult compact constructor).
    // The frontend just needs to not show the section in that case, not crash.
    const result = buildCouncilResult({ webSearchResults: [] });

    render(<ProcessDetails result={result} />);

    expect(screen.queryByText('🔍 Web Search Results')).not.toBeInTheDocument();
  });

  it('renders a heading and one card per result when webSearchResults is populated', () => {
    const result = buildCouncilResult({
      webSearchResults: [
        { title: 'Source 1', url: 'https://example.com/1', content: 'summary 1' },
        { title: 'Source 2', url: 'https://example.com/2', content: 'summary 2' },
      ],
    });

    render(<ProcessDetails result={result} />);

    expect(screen.getByText('🔍 Web Search Results')).toBeInTheDocument();
    const links = screen.getAllByRole('link');
    expect(links).toHaveLength(2);
    expect(links[0]).toHaveAttribute('href', 'https://example.com/1');
    expect(screen.getByText('Source 1')).toBeInTheDocument();
    expect(screen.getByText('Source 2')).toBeInTheDocument();
  });

  it('shows markdown content for a successful first opinion, the error message for a failed one', () => {
    const result = buildCouncilResult({
      firstOpinions: [
        buildLlmResponse({ providerName: 'model-a', success: true, content: 'successful opinion' }),
        buildLlmResponse({
          providerName: 'model-b',
          success: false,
          content: null,
          errorMessage: 'timed out',
        }),
      ],
    });

    render(<ProcessDetails result={result} />);

    expect(screen.getByText('successful opinion')).toBeInTheDocument();
    expect(screen.getByText('timed out')).toBeInTheDocument();
  });

  it('renders a card for every element in the reviews list', () => {
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
