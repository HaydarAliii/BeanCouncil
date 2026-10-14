import { vi } from 'vitest'

type MockResponseInit = {
  status?: number
  body?: unknown
  /** Simulates response.json() rejecting, as if the body isn't JSON / fails to parse. */
  jsonThrows?: boolean
}

/**
 * Stubs the global `fetch` to return a one-shot response and returns the mock function —
 * tests can then inspect `.mock.calls` to assert the requested URL/method/body.
 * `setup.ts`'s `afterEach` calls `vi.unstubAllGlobals()` after every test, so there's no
 * leakage between tests.
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
