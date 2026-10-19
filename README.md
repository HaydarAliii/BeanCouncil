# BeanCouncil

[![CI](https://github.com/HaydarAliii/BeanCouncil/actions/workflows/ci.yml/badge.svg)](https://github.com/HaydarAliii/BeanCouncil/actions/workflows/ci.yml)

`spring-boot` · `java` · `react` · `typescript` · `openrouter` · `llm` · `multi-agent` · `postgresql` · `vite` · `docker`

A multi-agent AI council app built on Java/Spring Boot + React, inspired by Andrej Karpathy's [llm-council](https://github.com/karpathy/llm-council) and jacob-bd's [the-ai-counsel](https://github.com/jacob-bd/the-ai-counsel). Spring Boot replaces the original Python/FastAPI architecture. Like the-ai-counsel, it talks to **only official APIs** — no reverse-engineered or keyless "free" services. The single key source is [OpenRouter](https://openrouter.ai): one OpenRouter API key gives official access to 400+ models (OpenAI, Anthropic, Google, xAI, and more).

Designed as a single-user tool — no multi-user/account system or per-user auth. Supports both local development and single-user (Basic Auth protected) deployment on your own server/VPS via Docker Compose.

## Architecture

The council logic is a 3-stage pipeline:

1. **First Opinions** — the user's question is sent in parallel to every selected council member except the president.
2. **Peer Review** — members' first answers are anonymized ("Response A/B/...") and each member critiques the others' answers.
3. **Final Synthesis** — **the council president never gives its own opinion**; it only reads the other members' opinions and critiques and produces the final answer (a pure judge/moderator role).

The result (first opinions + reviews + final answer) is persisted to the DB as a JSON transcript.

Council members and the president are **chosen by the user at runtime** — there's no hardcoded identity baked into code/config:

- The user enters their own OpenRouter API key from the app's **Settings** screen. The key is stored AES-256-GCM encrypted in a single-row `app_settings` table on the backend; the API never returns the full key, only a "present/absent" flag plus the last 4 characters.
- The Settings screen shows OpenRouter's live model catalog (400+ models) in real time: a free/paid badge, pricing, and the stored key's credit/limit status for each model.
- The user picks whichever models they want and makes one of them the president. A member's identity is simply its OpenRouter model id (e.g. `anthropic/claude-sonnet-5`).
- The council won't run until both a key and a model selection are in place; the backend returns `428 Precondition Required` with a clear error message in that case (never a silent `500`).

**Web search (optional)**: the user can also enter a free [Tavily](https://app.tavily.com/home) key from Settings (also AES-256-GCM encrypted, write-only). Once a key is stored, a "🔍 Search the web" option appears on the council screen — when checked, fresh search results from Tavily are added as context before the question is sent to the members. Fully optional: the option is hidden entirely if there's no key; if the search API errors out (invalid key, rate limit, network issue) it's silently skipped and the council still produces an answer as normal — search never breaks the flow.

Like council members and the president, conversations are **multi-turn (threaded)**: asking a new question in the same thread lets members see the previous turn(s) as context; an old conversation can be reopened from the History tab and continued from where it left off via "Continue this conversation."

## Project Structure

```
llmKonsey/
├── docker-compose.prod.yml   Full stack (Postgres+backend+frontend), for production deploy
├── .env.prod.example
├── backend/             Spring Boot API
│   ├── pom.xml
│   ├── Dockerfile
│   ├── docker-compose.yml   (Postgres only, for local development)
│   ├── .env.example
│   └── src/main/java/com/llmcouncil/
│       ├── adapter/     LlmProviderAdapter, OpenAiCompatibleAdapter (a single, generic OpenRouter adapter)
│       ├── config/      WebClient (including timeout + large response buffer)
│       ├── controller/  CouncilController, SettingsController, ModelsController, ConversationController
│       ├── exception/   SettingsNotConfiguredException, OpenRouterUnauthorizedException,
│       │                ConversationNotFoundException, GlobalExceptionHandler
│       ├── model/       dto (records) and JPA entities (including AppSettingsEntity, ConversationThreadEntity)
│       ├── repository/  Spring Data JPA repositories
│       ├── service/     CouncilService (the 3-stage pipeline + thread/follow-up + web search context),
│       │                CouncilMemberFactory (dynamic member construction), SettingsService,
│       │                OpenRouterCatalogService, WebSearchService (Tavily), ConversationHistoryService,
│       │                LegacyConversationMigration (one-time startup migration that moves legacy
│       │                records into threads)
│       └── util/        SecretCipher (AES-256-GCM key encryption)
│   └── src/test/java/com/llmcouncil/   69 tests — mirrors the src/main package structure (see "Tests")
└── frontend/             React + Vite + TypeScript UI
    ├── Dockerfile           (build + static serving/proxy via nginx)
    ├── nginx.conf           (Basic Auth + /api proxy)
    ├── docker-entrypoint.d/ (generates .htpasswd on container start)
    └── src/
        ├── api.ts        Type-safe communication with the backend
        └── components/   PromptForm, FinalAnswer, ProcessDetails, SettingsPage, KeyInputPanel
                           (shared OpenRouter+Tavily key input component), ModelPicker, HistoryPage
```

## Tech Stack

**Backend**
- Java 21, Spring Boot 4.1
- Spring Web (MVC, blocking) + WebClient (only for outbound LLM/OpenRouter calls, 30s timeout + 10MB response buffer)
- Spring Data JPA + PostgreSQL
- Spring Security Crypto (`Encryptors.delux`, AES-256-GCM) — key encryption
- Resilience4j (Retry, RateLimiter)
- Docker Compose (Postgres)
- `.env`-based configuration (spring-dotenv)

**Frontend**
- React 19 + TypeScript + Vite
- `react-markdown` + `remark-gfm` (model answers can contain markdown)
- On the dev server, `/api` requests are routed to the backend via the Vite proxy (no CORS needed)

## Running It

Requirements: Java 21+, Maven, Docker Desktop, Node 20+, an [OpenRouter](https://openrouter.ai/keys) account/key (free to create, no card required).

```bash
# Backend
cd backend
cp .env.example .env
# Generate APP_CRYPTO_SECRET / APP_CRYPTO_SALT and paste into .env:
openssl rand -base64 32   # -> APP_CRYPTO_SECRET
openssl rand -hex 16      # -> APP_CRYPTO_SALT
docker compose up -d      # Postgres
mvn spring-boot:run
```

```bash
# Frontend (separate terminal)
cd frontend
npm install
npm run dev                 # http://localhost:5173 (if the port is taken, Vite picks the next free one)
```

When you open the app in the browser and nothing's configured yet, you're automatically redirected to the **Settings** tab: enter your OpenRouter key, pick at least 2 models from the list, make one of them president, and save. After that you can ask questions from the **Council** tab.

To test the API directly (assuming settings are already configured):

```bash
curl -X POST http://localhost:8080/api/council/ask \
  -H "Content-Type: application/json" \
  -d '{"prompt":"hello"}'
```

**Note**: OpenRouter's `:free`-tagged models (~20 out of 400+) are completely free, but their shared pools can occasionally hit rate limits. Big-name models (gpt, claude, gemini, grok families) are always paid on OpenRouter — using them requires loading credit onto your account.

### Tests

```bash
cd backend
mvn test
```

69 tests. 51 of them are pure unit tests (no Spring context, no `.env`, finishing in ~1 second): the most critical logic (`CouncilService`: president neutrality, follow-up context, web search integration, graceful degradation on failures) is tested with pure Mockito; the 3 classes that make outbound HTTP calls (`OpenAiCompatibleAdapter`, `OpenRouterCatalogService`, `WebSearchService`) are tested against MockWebServer without touching the real network. The other 18 are `@DataJpaTest` integration tests against a real Postgres via **Testcontainers** — repository query methods, `IDENTITY` id generation, `TEXT` column round-trips, the single-row `app_settings` upsert, and the startup `LegacyConversationMigration` backfill, all verified against `postgres:16-alpine` instead of mocks. Running `mvn test` now requires a working **Docker daemon** (a single container is started once and shared across every integration test class).

```bash
cd frontend
npm test
```

53 tests (vitest + React Testing Library, on jsdom, passing in ~2 seconds with no browser/network needed): the error-parsing contract in `api.ts`, the `SettingsPage`/`App` state machines (view routing, clearing key inputs after save, thread continuity), and a regression guard for a bug that actually happened in this project — the "web search" section under "Show process" must not render at all when `webSearchResults` is empty. Component tests mock the `../api` module as a boundary; only `api.test.ts` stubs the real `fetch` call.

`.github/workflows/ci.yml` runs two jobs on every push/PR to `main`: backend (`mvn test`) and frontend (`npm run lint` + `npm test` + `npm run build`, including type checking). `ubuntu-latest` ships Docker preinstalled, so the backend job's Testcontainers tests run with no extra setup.

## Deploy (Docker Compose on your own server/VPS)

The root-level `docker-compose.prod.yml` brings up the entire app (Postgres + backend + frontend) with one command. The backend's and Postgres's ports are **never exposed externally** — only the frontend's nginx is exposed to the internet, and it sits behind **Basic Auth password protection** (since the server is internet-facing, this stops anyone else from spending your OpenRouter/Tavily credit). `/api` requests are proxied to the backend by nginx on the same origin, so CORS is never needed.

```bash
cp .env.prod.example .env.prod
# Fill in .env.prod:
openssl rand -base64 32   # -> APP_CRYPTO_SECRET
openssl rand -hex 16      # -> APP_CRYPTO_SALT
# Also set POSTGRES_PASSWORD, APP_AUTH_USERNAME, APP_AUTH_PASSWORD (don't use the dev defaults)

docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --build
```

Opening the server in a browser on the port you set via `APP_PORT` (default 80) brings up a Basic Auth prompt — after logging in with `APP_AUTH_USERNAME`/`APP_AUTH_PASSWORD`, the app itself appears. HTTPS is out of scope for this compose setup — if you have a domain, put a reverse proxy like Caddy/nginx/Cloudflare Tunnel in front and let it handle TLS (the `frontend` service only listens on HTTP).

Fully verified locally: `docker compose ... build` produces both images successfully, the containers come up, the backend connects to Postgres via the `postgres` service name (over the container network), requests without Basic Auth get `401`, and with the correct credentials both the static page and the `/api/*` proxy return `200`.

## Current Status

- ✅ Working: the 3-stage council pipeline end-to-end (first opinions → peer review → final synthesis), with the president synthesizing as a pure judge without taking part in the opinion/review stages
- ✅ Working: the user enters their own OpenRouter key and it's stored encrypted in the DB; models/president are freely chosen from the Settings screen
- ✅ Working: live model catalog on the Settings screen (free/paid badge, pricing, credit/limit status)
- ✅ Working: results persisted to the DB as a full transcript
- ✅ Working: React frontend — Council/Settings/History tabs, ask a question, see the final answer, inspect the intermediate stages via "Show process"
- ✅ Working: conversation history — threads are listed, and clicking one replays every turn inside it in order (first opinions, reviews, final answer); records left over from earlier architecture eras (g4f, hardcoded identities) are automatically migrated into their own threads on first app startup and open in a backward-compatible way
- ✅ Working: multi-turn (follow-up) conversations — asking a new question in the same thread lets council members see the previous turn(s) as context; a past conversation can also be "continued" from History right where it left off
- ✅ Working: optional web search (Tavily) — checking "Search the web" lets members see fresh search results as context; if there's no key or the search fails, the council silently continues without it and never crashes
- ✅ Working: production deploy — `docker-compose.prod.yml` brings up Postgres+backend+frontend with one command; backend/Postgres are closed off externally, frontend sits behind Basic Auth, no CORS needed (same-origin proxy)
- ✅ Working: backend test suite — 69 tests: 51 pure-Mockito/MockWebServer unit tests (`CouncilService`, `SettingsService`, `SecretCipher`, `LegacyConversationMigration`, `ConversationHistoryService`, `OpenAiCompatibleAdapter`, `OpenRouterCatalogService`, `WebSearchService`) plus 18 `@DataJpaTest` integration tests against a real Postgres via Testcontainers (repositories, id generation, the settings upsert, the legacy migration backfill)
- ✅ Working: frontend test suite — 53 tests (vitest/testing-library), component/state tests mocked at the `../api` boundary, passing in ~2s via `npm test` with no Docker
- ✅ Working: CI (GitHub Actions) — backend tests + frontend lint/test/build run automatically on every push/PR
- ❌ Not yet: Ollama support, automatic HTTPS (TLS is intentionally out of scope — bring your own reverse proxy)
