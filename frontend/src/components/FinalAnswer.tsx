import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import type { CouncilResult } from '../api';

type FinalAnswerProps = {
  result: CouncilResult;
};

export function FinalAnswer({ result }: FinalAnswerProps) {
  return (
    <section className="final-answer">
      <span className="president-badge">Council President: {result.presidentProvider}</span>
      <div className="markdown">
        <ReactMarkdown remarkPlugins={[remarkGfm]}>{result.finalAnswer}</ReactMarkdown>
      </div>
    </section>
  );
}
