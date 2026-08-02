# LLM Council

Andrej Karpathy'nin [llm-council](https://github.com/karpathy/llm-council) projesinden ilham alan, Java/Spring Boot tabanlı çoklu ajan (multi-agent) yapay zeka konsey uygulaması. Orijinal Python/FastAPI mimarisi yerine Spring Boot kullanılıyor; API maliyetlerini düşürmek için resmi ücretsiz API'lerin yanı sıra Docker üzerinden yönetilen bir "Reverse API" mikroservisiyle entegre çalışması hedefleniyor.

## Mimari

Konsey mantığı 3 aşamalı bir iş akışı olarak tasarlandı:

1. **First Opinions** — kullanıcı sorusu, kayıtlı tüm LLM sağlayıcılarına (Groq, Gemini, Reverse API, ...) paralel olarak gönderilir.
2. **Review** — modellerin ilk cevapları anonimleştirilir, modeller birbirinin cevabını eleştirir/puanlar.
3. **Final Sentez** — konsey başkanı (ana model) tüm fikir ve eleştirileri toplayıp nihai cevabı üretir.

Sağlayıcılar arası ortaklık `LlmProviderAdapter` arayüzü (Strategy/Adapter pattern) üzerinden sağlanır; her yeni sağlayıcı bu arayüzü implemente eden bir `@Component` olarak eklenir.

## Proje Yapısı

```
llmKonsey/
├── backend/            Spring Boot API
│   ├── pom.xml
│   ├── docker-compose.yml   (Postgres + reverse-api)
│   ├── .env.example
│   └── src/main/java/com/llmcouncil/
│       ├── adapter/     LlmProviderAdapter ve sağlayıcı implementasyonları
│       ├── config/      WebClient vb. bean tanımları
│       ├── controller/  REST endpoint'leri
│       ├── model/       dto (record) ve JPA entity'leri
│       ├── repository/  Spring Data JPA repository'leri
│       └── service/     Konsey iş akışı
└── frontend/            (henüz yok)
```

## Teknoloji Yığını

- Java 21, Spring Boot 4.1
- Spring Web (MVC, blocking) + WebClient (sadece dış LLM çağrıları için, uygulama uçtan uca reaktif değil)
- Spring Data JPA + PostgreSQL
- Resilience4j (Retry, RateLimiter)
- Docker Compose (Postgres + reverse-api placeholder)
- `.env` tabanlı konfigürasyon (spring-dotenv)

## Çalıştırma

Gereksinimler: Java 21+, Maven, Docker Desktop.

```bash
cd backend
cp .env.example .env        # GROQ_API_KEY değerini gir
docker compose up -d        # Postgres + reverse-api
mvn spring-boot:run
```

Test:

```bash
curl -X POST http://localhost:8080/api/council/ask \
  -H "Content-Type: application/json" \
  -d '{"prompt":"merhaba"}'
```

## Mevcut Durum

- ✅ Çalışıyor: Aşama 1 (first opinions) — tek sağlayıcı (`GroqAdapter`) ile paralel fan-out
- ❌ Henüz yok: Aşama 2 (review/anonimleştirme), Aşama 3 (final sentez)
- ❌ Henüz yok: ikinci bir adapter (Gemini, gerçek Reverse API), konuşma geçmişinin veritabanına kalıcı yazılması, testler, frontend
