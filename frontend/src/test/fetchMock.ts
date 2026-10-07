import { vi } from 'vitest'

type MockResponseInit = {
  status?: number
  body?: unknown
  /** gövde JSON değilse/parse hatası verirse response.json()'ın reddetmesini simüle eder. */
  jsonThrows?: boolean
}

/**
 * Global `fetch`'i tek seferlik bir yanıt döndürecek şekilde stub'lar ve mock fonksiyonunu
 * döner — testler `.mock.calls` ile istenen URL/method/body'yi doğrulayabilir.
 * `setup.ts`'teki `afterEach` her testten sonra `vi.unstubAllGlobals()` çağırır, bu yüzden
 * testler arasında sızıntı olmaz.
 */
export function mockFetchOnce({ status = 200, body, jsonThrows = false }: MockResponseInit = {}) {
  const fetchMock = vi.fn().mockResolvedValue({
    ok: status >= 200 && status < 300,
    status,
    json: () =>
      jsonThrows ? Promise.reject(new SyntaxError('Unexpected token')) : Promise.resolve(body),
  } as Response)
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}
