# BeanCouncil

`spring-boot` · `java` · `react` · `typescript` · `openrouter` · `llm` · `multi-agent` · `postgresql` · `vite` · `docker`

Andrej Karpathy'nin [llm-council](https://github.com/karpathy/llm-council) ve jacob-bd'nin [the-ai-counsel](https://github.com/jacob-bd/the-ai-counsel) projelerinden ilham alan, Java/Spring Boot + React tabanlı çoklu ajan (multi-agent) yapay zeka konsey uygulaması. Orijinal Python/FastAPI mimarisi yerine Spring Boot kullanılıyor. the-ai-counsel'daki gibi **tamamen resmi API'ler** üzerinden çalışır — hiçbir reverse-engineering/key'siz "ücretsiz" servis kullanılmaz. Tek key kaynağı [OpenRouter](https://openrouter.ai): tek bir OpenRouter API key'i ile 400'den fazla modele (OpenAI, Anthropic, Google, xAI ve daha fazlası) resmi şekilde erişilir.

Tek kullanıcılık bir araç olarak tasarlandı — çoklu kullanıcı/hesap sistemi veya kullanıcı bazlı kimlik doğrulama yok. Hem yerel geliştirme hem de kendi sunucunda/VPS'inde Docker Compose ile tek-kullanıcılık (Basic Auth korumalı) deploy desteklenir.

## Mimari

Konsey mantığı 3 aşamalı bir iş akışı olarak tasarlandı:

1. **First Opinions** — kullanıcı sorusu, başkan hariç tüm seçili konsey üyelerine paralel olarak gönderilir.
2. **Peer Review** — üyelerin ilk cevapları anonimleştirilir ("Response A/B/..."), her üye diğerlerinin cevabını eleştirir.
3. **Final Sentez** — **konsey başkanı kendi görüşünü hiç vermez**; sadece diğer üyelerin görüş ve eleştirilerini okuyup nihai cevabı üretir (saf hakem/moderatör rolü).

Sonuç (ilk görüşler + review'lar + final cevap) DB'ye JSON transcript olarak kaydedilir.

Konsey üyeleri ve başkan **kullanıcı tarafından çalışma zamanında seçilir** — koda/config'e gömülü sabit kimlik yoktur:

- Kullanıcı kendi OpenRouter API key'ini uygulamanın **Ayarlar** ekranından girer. Key, backend'de AES-256-GCM ile şifreli olarak tek-satırlık bir `app_settings` tablosunda saklanır; API hiçbir zaman key'i tam haliyle geri döndürmez (sadece "var/yok" + son 4 hane).
- Ayarlar ekranı OpenRouter'ın güncel model kataloğunu (400+ model) anlık olarak gösterir: her model için ücretsiz/ücretli rozeti, fiyat ve kayıtlı key'in kredi/limit durumu.
- Kullanıcı istediği modelleri seçip aralarından birini başkan yapar. Üye kimliği doğrudan OpenRouter model id'sidir (ör. `anthropic/claude-sonnet-5`).
- Hem key hem de model seçimi girilene kadar konsey çalışmaz; backend bu durumda `428 Precondition Required` + açık bir hata mesajı döner (sessizce `500` vermez).

**Web araştırması (opsiyonel)**: kullanıcı Ayarlar'dan ücretsiz bir [Tavily](https://app.tavily.com/home) key'i de girebilir (yine AES-256-GCM şifreli, write-only). Key kayıtlıysa konsey ekranında "🔍 Web'de ara" seçeneği belirir — işaretlenirse, soru üyelere gönderilmeden önce Tavily'den gelen güncel arama sonuçları bağlam olarak eklenir. Tamamen opsiyoneldir: key yoksa seçenek hiç görünmez; arama API'si hata verirse (geçersiz key, rate limit, ağ sorunu) sessizce atlanır ve konsey normal şekilde cevap üretmeye devam eder — arama hiçbir zaman akışı kesmez.

Konsey üyeleri ve başkan gibi, konuşmalar da **çok turlu (thread)**: aynı thread'e yeni bir soru sorulduğunda üyeler önceki tur(lar)ı bağlam olarak görür; Geçmiş sekmesinden eski bir konuşma açılıp "Bu konuşmaya devam et" ile kaldığı yerden sürdürülebilir.

## Proje Yapısı

```
llmKonsey/
├── docker-compose.prod.yml   Tam stack (Postgres+backend+frontend), production deploy için
├── .env.prod.example
├── backend/             Spring Boot API
│   ├── pom.xml
│   ├── Dockerfile
│   ├── docker-compose.yml   (sadece Postgres, yerel geliştirme için)
│   ├── .env.example
│   └── src/main/java/com/llmcouncil/
│       ├── adapter/     LlmProviderAdapter, OpenAiCompatibleAdapter (tek, generic OpenRouter adaptörü)
│       ├── config/      WebClient (timeout + büyük yanıt tamponu dahil)
│       ├── controller/  CouncilController, SettingsController, ModelsController, ConversationController
│       ├── exception/   SettingsNotConfiguredException, OpenRouterUnauthorizedException,
│       │                ConversationNotFoundException, GlobalExceptionHandler
│       ├── model/       dto (record) ve JPA entity'leri (AppSettingsEntity, ConversationThreadEntity dahil)
│       ├── repository/  Spring Data JPA repository'leri
│       ├── service/     CouncilService (3 aşama + thread/follow-up + web araması bağlamı),
│       │                CouncilMemberFactory (dinamik üye kurulumu), SettingsService,
│       │                OpenRouterCatalogService, WebSearchService (Tavily), ConversationHistoryService,
│       │                LegacyConversationMigration (eski kayıtları thread'lere taşıyan tek seferlik
│       │                başlangıç migration'ı)
│       └── util/        SecretCipher (AES-256-GCM key şifreleme)
└── frontend/             React + Vite + TypeScript arayüzü
    ├── Dockerfile           (build + nginx ile statik sunum/proxy)
    ├── nginx.conf           (Basic Auth + /api proxy)
    ├── docker-entrypoint.d/ (container başlarken .htpasswd üretir)
    └── src/
        ├── api.ts        Backend ile tip-güvenli iletişim
        └── components/   PromptForm, FinalAnswer, ProcessDetails, SettingsPage, KeyInputPanel
                           (OpenRouter+Tavily key girişi ortak bileşeni), ModelPicker, HistoryPage
```

## Teknoloji Yığını

**Backend**
- Java 21, Spring Boot 4.1
- Spring Web (MVC, blocking) + WebClient (sadece dış LLM/OpenRouter çağrıları için, 30sn timeout + 10MB yanıt tamponu ile)
- Spring Data JPA + PostgreSQL
- Spring Security Crypto (`Encryptors.delux`, AES-256-GCM) — key şifreleme
- Resilience4j (Retry, RateLimiter)
- Docker Compose (Postgres)
- `.env` tabanlı konfigürasyon (spring-dotenv)

**Frontend**
- React 19 + TypeScript + Vite
- `react-markdown` + `remark-gfm` (model cevapları markdown içerebiliyor)
- Dev sunucusunda `/api` istekleri Vite proxy ile backend'e yönlendirilir (CORS gerekmez)

## Çalıştırma

Gereksinimler: Java 21+, Maven, Docker Desktop, Node 20+, bir [OpenRouter](https://openrouter.ai/keys) hesabı/key'i (ücretsiz oluşturulur, kart istemez).

```bash
# Backend
cd backend
cp .env.example .env
# APP_CRYPTO_SECRET / APP_CRYPTO_SALT üret ve .env'e yapıştır:
openssl rand -base64 32   # -> APP_CRYPTO_SECRET
openssl rand -hex 16      # -> APP_CRYPTO_SALT
docker compose up -d      # Postgres
mvn spring-boot:run
```

```bash
# Frontend (ayrı terminalde)
cd frontend
npm install
npm run dev                 # http://localhost:5173 (port doluysa Vite otomatik sonraki boş portu seçer)
```

Tarayıcıda uygulamayı açtığında henüz ayar yapılmamışsa otomatik olarak **Ayarlar** sekmesine yönlendirilirsin: OpenRouter key'ini gir, listeden en az 2 model seç, birini başkan yap, kaydet. Bundan sonra **Konsey** sekmesinden soru sorabilirsin.

API'yi doğrudan test etmek için (ayarlar zaten yapılmışsa):

```bash
curl -X POST http://localhost:8080/api/council/ask \
  -H "Content-Type: application/json" \
  -d '{"prompt":"merhaba"}'
```

**Not**: OpenRouter'ın `:free` etiketli modelleri (400+ model içinden ~20 tanesi) tamamen ücretsizdir ama paylaşımlı havuzları zaman zaman rate-limit'e takılabilir. Büyük/isimli modeller (gpt, claude, gemini, grok ailesi) OpenRouter'da hep ücretlidir — bunları kullanmak için hesabına kredi yüklemen gerekir.

## Deploy (Docker Compose ile kendi sunucunda/VPS'inde)

Kökteki `docker-compose.prod.yml`, uygulamanın tamamını (Postgres + backend + frontend) tek komutla ayağa kaldırır. Backend ve Postgres'in portları **dışarıya hiç açılmaz** — internete sadece frontend'in nginx'i, o da **Basic Auth şifre koruması** arkasında açılır (sunucu internete açık olacağı için, başkasının senin OpenRouter/Tavily kredini kullanmasını engellemek amacıyla). `/api` istekleri nginx tarafından aynı origin üzerinden backend'e proxy'lenir — ayrı bir host olmadığı için CORS hiç gerekmez.

```bash
cp .env.prod.example .env.prod
# .env.prod'u doldur:
openssl rand -base64 32   # -> APP_CRYPTO_SECRET
openssl rand -hex 16      # -> APP_CRYPTO_SALT
# POSTGRES_PASSWORD, APP_AUTH_USERNAME, APP_AUTH_PASSWORD de gir (dev varsayılanlarını kullanma)

docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --build
```

Sunucunun `APP_PORT` ile belirttiğin porttan (varsayılan 80) tarayıcıyla açtığında Basic Auth ekranı karşına çıkar — `APP_AUTH_USERNAME`/`APP_AUTH_PASSWORD` ile giriş yaptıktan sonra uygulamanın kendisi görünür. HTTPS bu compose'un kapsamında değil — eğer bir alan adın varsa önüne Caddy/nginx/Cloudflare Tunnel gibi bir ters proxy koyup TLS'i ona bırakman yeterli (`frontend` servisi sadece HTTP dinler).

Tam yerel doğrulama yapıldı: `docker compose ... build` her iki imajı da başarıyla üretiyor, container'lar ayağa kalkıyor, backend Postgres'e `postgres` servis adıyla (container ağı üzerinden) bağlanıyor, Basic Auth'suz istekler `401`, doğru şifreyle hem statik sayfa hem `/api/*` proxy'si `200` dönüyor.

## Mevcut Durum

- ✅ Çalışıyor: 3 aşamalı konsey akışı uçtan uca (first opinions → peer review → final sentez), başkan ilk görüş/review aşamalarına katılmadan saf hakem olarak sentez yapıyor
- ✅ Çalışıyor: kullanıcı kendi OpenRouter key'ini girip DB'de şifreli saklıyor, modelleri/başkanı Ayarlar ekranından serbestçe seçiyor
- ✅ Çalışıyor: Ayarlar ekranında canlı model kataloğu (ücretsiz/ücretli rozeti, fiyat, kredi/limit durumu)
- ✅ Çalışıyor: sonuçların DB'ye tam transcript olarak kaydedilmesi
- ✅ Çalışıyor: React frontend — Konsey/Ayarlar/Geçmiş sekmeleri, soru sor, final cevabı gör, "Süreci göster" ile ara aşamaları incele
- ✅ Çalışıyor: konuşma geçmişi — thread'ler listelenir, herhangi birine tıklayınca içindeki tüm turlar (ilk görüşler, review'lar, final cevap) sırayla tekrar görüntülenir; eski mimari dönemlerden (g4f, sabit kimlikler) kalan kayıtlar da uygulama ilk açıldığında otomatik olarak kendi thread'lerine taşınıp geriye dönük uyumlu şekilde açılır
- ✅ Çalışıyor: çok turlu (follow-up) konuşmalar — aynı thread'e yeni bir soru sorulduğunda konsey üyeleri önceki tur(lar)ı bağlam olarak görür; geçmişten de bir konuşmaya "devam et" ile kaldığı yerden sürdürülebilir
- ✅ Çalışıyor: opsiyonel web araştırması (Tavily) — "Web'de ara" işaretlenince üyeler güncel arama sonuçlarını bağlam olarak görür; key yoksa veya arama başarısız olursa konsey sessizce aramasız devam eder, hiçbir zaman çökmez
- ✅ Çalışıyor: production deploy — `docker-compose.prod.yml` ile tek komutla Postgres+backend+frontend; backend/Postgres dışa kapalı, frontend Basic Auth arkasında, CORS'a gerek yok (aynı origin proxy)
- ❌ Henüz yok: testler, Ollama desteği, otomatik HTTPS (TLS bilinçli olarak kapsam dışı — kullanıcı kendi ters proxy'sini koyar)
