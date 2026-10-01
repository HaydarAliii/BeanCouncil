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

export type SettingsResponse = {
  hasKey: boolean;
  keyPreview: string | null;
  selectedModelIds: string[];
  presidentModelId: string | null;
};

export type SettingsRequest = {
  openRouterKey?: string;
  selectedModelIds: string[];
  presidentModelId: string;
};

export type ModelInfo = {
  id: string;
  name: string;
  contextLength: number | null;
  promptPrice: string;
  completionPrice: string;
  isFree: boolean;
};

export type KeyStatus = {
  label: string | null;
  limit: number | null;
  usage: number | null;
  isFreeTier: boolean;
  limitRemaining: number | null;
};

/** Backend hata gövdesi `{ error: "..." }` ise onu, yoksa HTTP durumunu mesaj olarak kullanır. */
async function handle<T>(response: Response, fallbackMessage: string): Promise<T> {
  if (!response.ok) {
    let message = `${fallbackMessage} (HTTP ${response.status})`;
    try {
      const body = await response.json();
      if (body && typeof body.error === 'string') {
        message = body.error;
      }
    } catch {
      // gövde JSON değilse HTTP durumunu kullanmaya devam et
    }
    throw new Error(message);
  }
  return response.json();
}

export async function askCouncil(prompt: string): Promise<CouncilResult> {
  const response = await fetch('/api/council/ask', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ prompt }),
  });
  return handle(response, 'Konsey isteği başarısız oldu');
}

export async function getSettings(): Promise<SettingsResponse> {
  const response = await fetch('/api/settings');
  return handle(response, 'Ayarlar alınamadı');
}

export async function saveSettings(request: SettingsRequest): Promise<SettingsResponse> {
  const response = await fetch('/api/settings', {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  });
  return handle(response, 'Ayarlar kaydedilemedi');
}

export async function getModels(): Promise<ModelInfo[]> {
  const response = await fetch('/api/models');
  const data = await handle<{ models: ModelInfo[] }>(response, 'Model listesi alınamadı');
  return data.models;
}

export async function getKeyStatus(): Promise<KeyStatus> {
  const response = await fetch('/api/models/key');
  return handle(response, 'Key durumu alınamadı');
}
