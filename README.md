# LLM Council

Andrej Karpathy'nin [llm-council](https://github.com/karpathy/llm-council) ve jacob-bd'nin [the-ai-counsel](https://github.com/jacob-bd/the-ai-counsel) projelerinden ilham alan, Java/Spring Boot + React tabanlı çoklu ajan (multi-agent) yapay zeka konsey uygulaması. Orijinal Python/FastAPI mimarisi yerine Spring Boot kullanılıyor. the-ai-counsel'daki gibi **tamamen resmi API'ler** üzerinden çalışır — hiçbir reverse-engineering/key'siz "ücretsiz" servis kullanılmaz.

## Mimari

Konsey mantığı 3 aşamalı bir iş akışı olarak tasarlandı:

1. **First Opinions** — kullanıcı sorusu, kayıtlı tüm LLM sağlayıcılarına paralel olarak gönderilir.
2. **Peer Review** — modellerin ilk cevapları anonimleştirilir ("Response A/B/..."), her model diğerlerinin cevabını eleştirir.
3. **Final Sentez** — sabit bir "konsey başkanı" model, tüm görüş ve eleştirileri toplayıp nihai cevabı üretir.

Sonuç (ilk görüşler + review'lar + final cevap) DB'ye JSON transcript olarak kaydedilir.

Konsey 4 sabit kimlikten oluşur: **gpt, gemini, claude, grok**. Her kimlik `LlmProviderAdapter` arayüzü (Strategy/Adapter pattern) üzerinden, önce kendi resmi/direkt sağlayıcısını (key varsa) dener; o yoksa [OpenRouter](https://openrouter.ai) üzerinden aynı modele ulaşmayı dener (`FallbackAdapter`, Decorator pattern). İkisi de yoksa o kimlik konseyde hiç görünmez — hiçbir zaman sahte/yanıltıcı bir yedek kullanılmaz.

- **gpt** → Groq (`openai/gpt-oss-120b`, ücretsiz katman) veya OpenRouter'ın gerçekten ücretsiz `openai/gpt-oss-20b:free` modeli
- **gemini** → Google AI Studio (ücretsiz katman) veya OpenRouter
- **claude**, **grok** → resmi Anthropic/xAI API key'i veya OpenRouter bakiyesi gerekir — bu ikisinin hiçbir yerde ücretsiz API'si yok, bilinçli bir sınırlama

## Proje Yapısı

```
llmKonsey/
├── backend/             Spring Boot API
│   ├── pom.xml
│   ├── docker-compose.yml   (Postgres)
│   ├── .env.example
│   └── src/main/java/com/llmcouncil/
│       ├── adapter/     LlmProviderAdapter, OpenAiCompatibleAdapter, ClaudeAdapter, GeminiAdapter, FallbackAdapter
│       ├── config/      WebClient (timeout dahil), CouncilProvidersConfig (4 kimlik bean'i)
│       ├── controller/  REST endpoint'leri
│       ├── model/       dto (record) ve JPA entity'leri
│       ├── repository/  Spring Data JPA repository'leri
│       └── service/     Konsey iş akışı (3 aşama + persistence)
└── frontend/             React + Vite + TypeScript arayüzü
    └── src/
        ├── api.ts        Backend ile tip-güvenli iletişim
        └── components/   PromptForm, FinalAnswer, ProcessDetails
```

## Teknoloji Yığını

**Backend**
- Java 21, Spring Boot 4.1
- Spring Web (MVC, blocking) + WebClient (sadece dış LLM çağrıları için, 30sn timeout ile)
- Spring Data JPA + PostgreSQL
- Resilience4j (Retry, RateLimiter)
- Docker Compose (Postgres)
- `.env` tabanlı konfigürasyon (spring-dotenv)

**Frontend**
- React 19 + TypeScript + Vite
- `react-markdown` + `remark-gfm` (model cevapları markdown içerebiliyor)
- Dev sunucusunda `/api` istekleri Vite proxy ile backend'e yönlendirilir (CORS gerekmez)

## Çalıştırma

Gereksinimler: Java 21+, Maven, Docker Desktop, Node 20+.

```bash
# Backend
cd backend
cp .env.example .env        # en az GROQ_API_KEY veya GEMINI_API_KEY gir (ikisi de ücretsiz, kart istemez)
docker compose up -d        # Postgres
mvn spring-boot:run
```

```bash
# Frontend (ayrı terminalde)
cd frontend
npm install
npm run dev                 # http://localhost:5173
```

API'yi doğrudan test etmek için:

```bash
curl -X POST http://localhost:8080/api/council/ask \
  -H "Content-Type: application/json" \
  -d '{"prompt":"merhaba"}'
```

**Not**: tek bir key ile (örn. sadece `GROQ_API_KEY`) konsey tek üyeli çalışır — peer review anlamsızlaşır. Gerçek bir konsey deneyimi için en az `GROQ_API_KEY` + `GEMINI_API_KEY` (ikisi de ücretsiz) girilmesi önerilir. `claude`/`grok`'un aktif olması için `ANTHROPIC_API_KEY`/`XAI_API_KEY` veya `OPENROUTER_API_KEY` (bakiye yüklenmiş) gerekir.

## Mevcut Durum

- ✅ Çalışıyor: 3 aşamalı konsey akışı uçtan uca (first opinions → peer review → final sentez), 4 sabit kimlik (gpt/gemini/claude/grok), her biri resmi API → OpenRouter fallback deseniyle
- ✅ Çalışıyor: sonuçların DB'ye tam transcript olarak kaydedilmesi
- ✅ Çalışıyor: React frontend — soru sor, final cevabı gör, "Süreci göster" ile ara aşamaları incele
- ❌ Henüz yok: testler, konuşma geçmişi listeleme (backend'de kayıtlı ama UI/endpoint yok), prod deploy/CORS ayarları, Ollama desteği
