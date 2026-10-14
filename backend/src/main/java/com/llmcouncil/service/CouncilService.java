package com.llmcouncil.service;

import com.llmcouncil.adapter.LlmProviderAdapter;
import com.llmcouncil.exception.ConversationNotFoundException;
import com.llmcouncil.model.dto.AppSettings;
import com.llmcouncil.model.dto.CouncilResult;
import com.llmcouncil.model.dto.LlmResponse;
import com.llmcouncil.model.dto.WebSearchResult;
import com.llmcouncil.model.entity.ConversationEntity;
import com.llmcouncil.model.entity.ConversationThreadEntity;
import com.llmcouncil.repository.ConversationRepository;
import com.llmcouncil.repository.ConversationThreadRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * The council workflow has 3 stages: first opinions (parallel fan-out, president EXCLUDED) →
 * peer review (anonymized mutual critique, president EXCLUDED) → final synthesis (president
 * only). The president no longer gives its own opinion or critiques anyone — it's a pure judge
 * role that just listens to the other members and renders the final verdict. Members and the
 * president are built dynamically on every call from the user's saved settings (see
 * {@link CouncilMemberFactory}). The result is persisted along with its full process (transcript).
 */
@Service
public class CouncilService {

    private static final Logger log = LoggerFactory.getLogger(CouncilService.class);

    private final CouncilMemberFactory memberFactory;
    private final SettingsService settingsService;
    private final WebSearchService webSearchService;
    private final ConversationRepository repository;
    private final ConversationThreadRepository threadRepository;
    private final ObjectMapper objectMapper;

    public CouncilService(CouncilMemberFactory memberFactory,
                           SettingsService settingsService,
                           WebSearchService webSearchService,
                           ConversationRepository repository,
                           ConversationThreadRepository threadRepository,
                           ObjectMapper objectMapper) {
        this.memberFactory = memberFactory;
        this.settingsService = settingsService;
        this.webSearchService = webSearchService;
        this.repository = repository;
        this.threadRepository = threadRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * If {@code threadId} is null, a new conversation is started; if set, this is appended to
     * that thread as a follow-up. When {@code webSearch} is true and a Tavily key is configured,
     * members see current web results as context before answering (if the search fails, it's
     * silently skipped).
     */
    public CouncilResult deliberate(String prompt, Long threadId, boolean webSearch) {
        long resolvedThreadId = resolveThread(threadId, prompt);
        List<ConversationEntity> history = repository.findByThreadIdOrderByCreatedAtAsc(resolvedThreadId);

        AppSettings settings = settingsService.getDecryptedSettingsOrThrow();
        String presidentId = settings.presidentModelId();

        List<WebSearchResult> searchResults = (webSearch && settings.hasTavilyKey())
                ? webSearchService.search(prompt, settings.tavilyKey())
                : List.of();

        String effectivePrompt = buildContextualPrompt(history, searchResults, prompt);

        List<LlmProviderAdapter> allMembers = memberFactory.buildAllMembers(settings);

        LlmProviderAdapter president = allMembers.stream()
                .filter(member -> member.getProviderName().equals(presidentId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "President model ('" + presidentId + "') was not found among the selected members."));

        List<LlmProviderAdapter> councilors = allMembers.stream()
                .filter(member -> !member.getProviderName().equals(presidentId))
                .toList();

        List<LlmResponse> firstOpinions = collectFirstOpinions(councilors, effectivePrompt);

        List<LlmResponse> reviews;
        String actualPresident;
        String finalAnswer;
        if (firstOpinions.stream().anyMatch(LlmResponse::success)) {
            reviews = collectReviews(councilors, effectivePrompt, firstOpinions);
            SynthesisResult synthesis = synthesize(president, councilors, effectivePrompt, firstOpinions, reviews);
            actualPresident = synthesis.providerName();
            finalAnswer = synthesis.finalAnswer();
        } else {
            reviews = List.of();
            actualPresident = presidentId;
            finalAnswer = "The council could not produce an answer because all members failed.";
        }

        CouncilResult result = new CouncilResult(prompt, firstOpinions, reviews, actualPresident, finalAnswer,
                resolvedThreadId, searchResults);
        persist(result, resolvedThreadId);
        return result;
    }

    /** @return the existing thread's id, or the newly created thread's id. */
    private long resolveThread(Long threadId, String firstPrompt) {
        if (threadId != null) {
            ConversationThreadEntity thread = threadRepository.findById(threadId)
                    .orElseThrow(() -> new ConversationNotFoundException("Conversation not found: " + threadId));
            thread.touch();
            threadRepository.save(thread);
            return threadId;
        }
        return threadRepository.save(new ConversationThreadEntity(firstPrompt)).getId();
    }

    /**
     * Prepends prior turns and/or web search results (if any) as context; if neither is present,
     * returns the question as-is.
     */
    private String buildContextualPrompt(List<ConversationEntity> history, List<WebSearchResult> searchResults,
                                          String latestPrompt) {
        StringBuilder context = new StringBuilder();

        if (!history.isEmpty()) {
            String historyBlock = history.stream()
                    .map(turn -> "User: " + turn.getPrompt() + "\nCouncil: " + turn.getFinalAnswer())
                    .collect(Collectors.joining("\n\n"));
            context.append("This is part of an ongoing conversation. Previous turn(s):\n\n")
                    .append(historyBlock)
                    .append("\n\n");
        }

        if (!searchResults.isEmpty()) {
            String searchBlock = searchResults.stream()
                    .map(r -> "- %s (%s)\n  %s".formatted(r.title(), r.url(), r.content()))
                    .collect(Collectors.joining("\n\n"));
            context.append("Current web search results (use them if helpful when answering; no need to cite sources):\n\n")
                    .append(searchBlock)
                    .append("\n\n");
        }

        if (context.isEmpty()) {
            return latestPrompt;
        }
        context.append(history.isEmpty() ? "Question: " : "User's new question: ").append(latestPrompt);
        return context.toString();
    }

    private List<LlmResponse> collectFirstOpinions(List<LlmProviderAdapter> councilors, String prompt) {
        List<CompletableFuture<LlmResponse>> futures = councilors.stream()
                .map(provider -> provider.generateResponse(prompt))
                .toList();

        return futures.stream()
                .map(CompletableFuture::join)
                .toList();
    }

    private List<LlmResponse> collectReviews(List<LlmProviderAdapter> councilors, String prompt, List<LlmResponse> firstOpinions) {
        List<AnonymizedOpinion> anonymized = anonymize(firstOpinions);

        List<CompletableFuture<LlmResponse>> futures = councilors.stream()
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

    private SynthesisResult synthesize(LlmProviderAdapter president, List<LlmProviderAdapter> councilors,
                                        String prompt, List<LlmResponse> firstOpinions, List<LlmResponse> reviews) {
        String opinionsBlock = formatBlock(anonymize(firstOpinions));
        String reviewsBlock = reviews.stream()
                .filter(LlmResponse::success)
                .map(review -> review.providerName() + "'s review:\n" + review.content())
                .collect(Collectors.joining("\n\n"));

        String synthesisPrompt = """
                You are the president of a council of AI models. You did NOT give your own answer — \
                your role is purely to listen and judge. Below are the council members' initial \
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
        for (LlmProviderAdapter fallback : councilors) {
            LlmResponse fallbackResponse = fallback.generateResponse(synthesisPrompt).join();
            if (fallbackResponse.success()) {
                return new SynthesisResult(fallback.getProviderName(), fallbackResponse.content());
            }
        }
        return new SynthesisResult(president.getProviderName(), "President provider failed: " + response.errorMessage());
    }

    private record SynthesisResult(String providerName, String finalAnswer) {
    }

    private void persist(CouncilResult result, long threadId) {
        ConversationEntity entity = new ConversationEntity(result.prompt());
        entity.setThreadId(threadId);
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
