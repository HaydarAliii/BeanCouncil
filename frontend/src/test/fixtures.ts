import type { CouncilResult, LlmResponse } from '../api';

export function buildLlmResponse(overrides: Partial<LlmResponse> = {}): LlmResponse {
  return {
    providerName: 'model-a',
    content: 'cevap içeriği',
    success: true,
    errorMessage: null,
    ...overrides,
  };
}

export function buildCouncilResult(overrides: Partial<CouncilResult> = {}): CouncilResult {
  return {
    prompt: 'soru',
    firstOpinions: [],
    reviews: [],
    presidentProvider: 'model-president',
    finalAnswer: 'final cevap',
    threadId: 1,
    webSearchResults: [],
    ...overrides,
  };
}
