export type LlmResponse = {
  providerName: string;
  content: string | null;
  success: boolean;
  errorMessage: string | null;
};

export type CouncilResult = {
  prompt: string;
  firstOpinions: LlmResponse[];
  reviews: LlmResponse[];
  presidentProvider: string;
  finalAnswer: string;
};

export async function askCouncil(prompt: string): Promise<CouncilResult> {
  const response = await fetch('/api/council/ask', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ prompt }),
  });

  if (!response.ok) {
    throw new Error(`Konsey isteği başarısız oldu (HTTP ${response.status})`);
  }

  return response.json();
}
