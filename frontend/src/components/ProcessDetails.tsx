import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import type { CouncilResult, LlmResponse } from '../api';

type ProcessDetailsProps = {
  result: CouncilResult;
};

function OpinionCard({ opinion }: { opinion: LlmResponse }) {
  return (
    <article className="opinion-card">
      <h4>{opinion.providerName}</h4>
      {opinion.success ? (
        <div className="markdown">
          <ReactMarkdown remarkPlugins={[remarkGfm]}>{opinion.content ?? ''}</ReactMarkdown>
        </div>
      ) : (
        <p className="error-text">{opinion.errorMessage}</p>
      )}
    </article>
  );
}

export function ProcessDetails({ result }: ProcessDetailsProps) {
  return (
    <details className="process-details">
      <summary>Süreci göster</summary>

      <h3>İlk Görüşler</h3>
      <div className="opinion-grid">
        {result.firstOpinions.map((opinion) => (
          <OpinionCard key={opinion.providerName} opinion={opinion} />
        ))}
      </div>

      <h3>Karşılıklı Değerlendirme</h3>
      <div className="opinion-grid">
        {result.reviews.map((review, index) => (
          <OpinionCard key={`${review.providerName}-${index}`} opinion={review} />
        ))}
      </div>
    </details>
  );
}
