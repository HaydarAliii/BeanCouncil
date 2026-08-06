package com.llmcouncil.service;

import com.llmcouncil.adapter.LlmProviderAdapter;
import com.llmcouncil.model.dto.CouncilResult;
import com.llmcouncil.model.dto.LlmResponse;
import com.llmcouncil.model.entity.ConversationEntity;
import com.llmcouncil.repository.ConversationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Konsey iş akışının 3 aşaması: first opinions (paralel fan-out) → peer review
 * (anonimleştirilmiş karşılıklı eleştiri) → final sentez (sabit başkan model).
 * Sonuç, tam süreciyle (transcript) birlikte kalıcı hale getirilir.
 */
@Service
public class CouncilService {

    private static final Logger log = LoggerFactory.getLogger(CouncilService.class);

    private final List<LlmProviderAdapter> providers;
    private final Map<String, LlmProviderAdapter> providersByName;
    private final String presidentProviderName;
    private final ConversationRepository repository;
    private final ObjectMapper objectMapper;

    public CouncilService(List<LlmProviderAdapter> providers,
                           @Value("${llm.council.president}") String presidentProviderName,
                           ConversationRepository repository,
                           ObjectMapper objectMapper) {
        this.providers = providers;
        this.providersByName = providers.stream()
                .collect(Collectors.toMap(LlmProviderAdapter::getProviderName, p -> p));
        this.presidentProviderName = presidentProviderName;
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public CouncilResult deliberate(String prompt) {
        List<LlmResponse> firstOpinions = collectFirstOpinions(prompt);

        List<LlmResponse> reviews;
        String actualPresident;
        String finalAnswer;
        if (firstOpinions.stream().anyMatch(LlmResponse::success)) {
            reviews = collectReviews(prompt, firstOpinions);
            SynthesisResult synthesis = synthesize(resolvePresident(), prompt, firstOpinions, reviews);
            actualPresident = synthesis.providerName();
            finalAnswer = synthesis.finalAnswer();
        } else {
            reviews = List.of();
            actualPresident = presidentProviderName;
            finalAnswer = "The council could not produce an answer because all providers failed.";
        }

        CouncilResult result = new CouncilResult(prompt, firstOpinions, reviews, actualPresident, finalAnswer);
        persist(result);
        return result;
    }

    public List<LlmResponse> collectFirstOpinions(String prompt) {
        List<CompletableFuture<LlmResponse>> futures = providers.stream()
                .map(provider -> provider.generateResponse(prompt))
                .toList();

        return futures.stream()
                .map(CompletableFuture::join)
                .toList();
    }

    private List<LlmResponse> collectReviews(String prompt, List<LlmResponse> firstOpinions) {
        List<AnonymizedOpinion> anonymized = anonymize(firstOpinions);

        List<CompletableFuture<LlmResponse>> futures = providers.stream()
                .map(provider -> buildReviewFuture(provider, prompt, anonymized))
                .toList();

        return futures.stream()
                .map(CompletableFuture::join)
                .toList();
    }

    private CompletableFuture<LlmResponse> buildReviewFuture(LlmProviderAdapter provider,
                                                               String prompt,
                                                               List<AnonymizedOpinion> anonymized) {
        List<AnonymizedOpinion> others = anonymized.stream()
                .filter(opinion -> !opinion.providerName().equals(provider.getProviderName()))
                .toList();

        if (others.isEmpty()) {
            return CompletableFuture.completedFuture(
                    LlmResponse.ok(provider.getProviderName(), "No peer opinions available to review."));
        }

        String reviewPrompt = """
                You are one member of a council of AI models answering the same question. \
                Below are anonymized answers from the other council members. \
                Briefly critique their strengths and weaknesses, and note which one you find most convincing.

                Original question: %s

                %s
                """.formatted(prompt, formatBlock(others));

        return provider.generateResponse(reviewPrompt);
    }

    private SynthesisResult synthesize(LlmProviderAdapter president, String prompt, List<LlmResponse> firstOpinions, List<LlmResponse> reviews) {
        String opinionsBlock = formatBlock(anonymize(firstOpinions));
        String reviewsBlock = reviews.stream()
                .filter(LlmResponse::success)
                .map(review -> review.providerName() + "'s review:\n" + review.content())
                .collect(Collectors.joining("\n\n"));

        String synthesisPrompt = """
                You are the president of a council of AI models. Below are the council members' initial \
                answers and their peer reviews of each other. Synthesize a single best final answer to the \
                original question, drawing on the strongest points raised.

                Original question: %s

                Initial answers:
                %s

                Peer reviews:
                %s
                """.formatted(prompt, opinionsBlock, reviewsBlock);

        LlmResponse response = president.generateResponse(synthesisPrompt).join();
        if (response.success()) {
            return new SynthesisResult(president.getProviderName(), response.content());
        }

        log.warn("Council president '{}' failed to synthesize ({}); trying other members",
                president.getProviderName(), response.errorMessage());
        for (LlmProviderAdapter fallback : providers) {
            if (fallback == president) {
                continue;
            }
            LlmResponse fallbackResponse = fallback.generateResponse(synthesisPrompt).join();
            if (fallbackResponse.success()) {
                return new SynthesisResult(fallback.getProviderName(), fallbackResponse.content());
            }
        }
        return new SynthesisResult(president.getProviderName(), "President provider failed: " + response.errorMessage());
    }

    private record SynthesisResult(String providerName, String finalAnswer) {
    }

    private LlmProviderAdapter resolvePresident() {
        LlmProviderAdapter configured = providersByName.get(presidentProviderName);
        if (configured != null) {
            return configured;
        }
        if (providers.isEmpty()) {
            throw new IllegalStateException("No LLM providers are registered; check .env / application.yml");
        }
        LlmProviderAdapter fallback = providers.get(0);
        log.warn("Configured council president '{}' has no matching provider; falling back to '{}'",
                presidentProviderName, fallback.getProviderName());
        return fallback;
    }

    private void persist(CouncilResult result) {
        ConversationEntity entity = new ConversationEntity(result.prompt());
        entity.setFinalAnswer(result.finalAnswer());
        entity.setTranscript(objectMapper.writeValueAsString(result));
        repository.save(entity);
    }

    private List<AnonymizedOpinion> anonymize(List<LlmResponse> opinions) {
        List<AnonymizedOpinion> anonymized = new ArrayList<>();
        int index = 0;
        for (LlmResponse opinion : opinions) {
            if (opinion.success()) {
                anonymized.add(new AnonymizedOpinion("Response " + (char) ('A' + index), opinion.providerName(), opinion.content()));
                index++;
            }
        }
        return anonymized;
    }

    private String formatBlock(List<AnonymizedOpinion> opinions) {
        return opinions.stream()
                .map(opinion -> opinion.label() + ":\n" + opinion.content())
                .collect(Collectors.joining("\n\n"));
    }

    private record AnonymizedOpinion(String label, String providerName, String content) {
    }
}
