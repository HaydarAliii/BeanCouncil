package com.llmcouncil.service;

import com.llmcouncil.model.dto.WebSearchResult;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the Tavily contract — specifically the "search never breaks the council" design
 * promise: every kind of failure (401, malformed response, connection drop) must result in an
 * empty list, and an exception must NEVER escape.
 */
class WebSearchServiceTest {

    MockWebServer server;
    WebSearchService service;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        service = new WebSearchService(WebClient.builder(), server.url("/").toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        server.close();
    }

    @Test
    void search_success_mapsResultsAndSendsExpectedRequest() throws InterruptedException {
        server.enqueue(new MockResponse.Builder()
                .addHeader("Content-Type", "application/json")
                .body("""
                        {"results":[{"title":"T","url":"http://u","content":"C"}]}""")
                .build());

        List<WebSearchResult> results = service.search("question", "tvly-key");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).title()).isEqualTo("T");
        assertThat(results.get(0).url()).isEqualTo("http://u");
        assertThat(results.get(0).content()).isEqualTo("C");

        RecordedRequest request = server.takeRequest();
        assertThat(request.getTarget()).isEqualTo("/search");
        assertThat(request.getHeaders().get("Authorization")).isEqualTo("Bearer tvly-key");
        String body = request.getBody().utf8();
        assertThat(body).contains("\"max_results\":5").contains("\"search_depth\":\"basic\"").contains("\"query\":\"question\"");
    }

    @Test
    void search_401_returnsEmptyList_neverThrows() {
        server.enqueue(new MockResponse.Builder().code(401).build());

        List<WebSearchResult> results = service.search("question", "bad-key");

        assertThat(results).isEmpty();
    }

    @Test
    void search_malformedJson_returnsEmptyList_neverThrows() {
        server.enqueue(new MockResponse.Builder()
                .addHeader("Content-Type", "application/json")
                .body("this is not valid json")
                .build());

        List<WebSearchResult> results = service.search("question", "key");

        assertThat(results).isEmpty();
    }

    @Test
    void search_missingResultsField_returnsEmptyList() {
        server.enqueue(new MockResponse.Builder()
                .addHeader("Content-Type", "application/json")
                .body("""
                        {"query":"question"}""")
                .build());

        List<WebSearchResult> results = service.search("question", "key");

        assertThat(results).isEmpty();
    }

    @Test
    void search_connectionFailure_returnsEmptyList_neverThrows() throws IOException {
        server.close(); // with the server down, the request blows up with a connection error

        List<WebSearchResult> results = service.search("question", "key");

        assertThat(results).isEmpty();
    }
}
