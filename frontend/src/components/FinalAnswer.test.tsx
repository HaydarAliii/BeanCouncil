import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { FinalAnswer } from './FinalAnswer';
import { buildCouncilResult } from '../test/fixtures';

describe('FinalAnswer', () => {
  it('shows the president badge', () => {
    render(<FinalAnswer result={buildCouncilResult({ presidentProvider: 'anthropic/claude-sonnet-5' })} />);

    expect(screen.getByText('Council President: anthropic/claude-sonnet-5')).toBeInTheDocument();
  });

  it('renders the finalAnswer markdown content', () => {
    render(<FinalAnswer result={buildCouncilResult({ finalAnswer: 'final answer text' })} />);

    expect(screen.getByText('final answer text')).toBeInTheDocument();
  });
});
