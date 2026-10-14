import type { CouncilResult, LlmResponse } from '../api';

export function buildLlmResponse(overrides: Partial<LlmResponse> = {}): LlmResponse {
  return {
    providerName: 'model-a',
    content: 'answer content',
    success: true,
    errorMessage: null,
    ...overrides,
  };
}

export function buildCouncilResult(overrides: Partial<CouncilResult> = {}): CouncilResult {
  return {
    prompt: 'question',
    firstOpinions: [],
    reviews: [],
    presidentProvider: 'model-president',
    finalAnswer: 'final answer',
    threadId: 1,
    webSearchResults: [],
    ...overrides,
  };
}
