import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { FinalAnswer } from './FinalAnswer';
import { buildCouncilResult } from '../test/fixtures';

describe('FinalAnswer', () => {
  it('başkan rozetini gösterir', () => {
    render(<FinalAnswer result={buildCouncilResult({ presidentProvider: 'anthropic/claude-sonnet-5' })} />);

    expect(screen.getByText('Konsey Başkanı: anthropic/claude-sonnet-5')).toBeInTheDocument();
  });

  it('finalAnswer markdown içeriğini render eder', () => {
    render(<FinalAnswer result={buildCouncilResult({ finalAnswer: 'nihai cevap metni' })} />);

    expect(screen.getByText('nihai cevap metni')).toBeInTheDocument();
  });
});
