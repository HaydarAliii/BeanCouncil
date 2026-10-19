package com.llmcouncil.service;

import com.llmcouncil.model.entity.ConversationEntity;
import com.llmcouncil.model.entity.ConversationThreadEntity;
import com.llmcouncil.repository.ConversationRepository;
import com.llmcouncil.repository.ConversationThreadRepository;
import com.llmcouncil.support.AbstractPostgresTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link LegacyConversationMigration} against a real Postgres
 * (Testcontainers) — complements the pure-Mockito {@code LegacyConversationMigrationTest},
 * which verifies the same logic against mocked repositories.
 */
class LegacyConversationMigrationIntegrationTest extends AbstractPostgresTest {

    @Autowired
    ConversationRepository conversationRepository;
    @Autowired
    ConversationThreadRepository threadRepository;

    private LegacyConversationMigration migration;

    private LegacyConversationMigration migration() {
        if (migration == null) {
            migration = new LegacyConversationMigration(conversationRepository, threadRepository);
        }
        return migration;
    }

    private ConversationEntity orphan(String prompt, Instant createdAt) {
        ConversationEntity entity = new ConversationEntity(prompt);
        ReflectionTestUtils.setField(entity, "createdAt", createdAt);
        return conversationRepository.save(entity);
    }

    @Test
    void run_withOrphans_createsThreadsAndBackfillsThreadId() {
        Instant createdAt1 = Instant.parse("2026-01-01T00:00:00Z");
        Instant createdAt2 = Instant.parse("2026-01-02T00:00:00Z");
        Long orphan1Id = (Long) ReflectionTestUtils.getField(orphan("old question 1", createdAt1), "id");
        Long orphan2Id = (Long) ReflectionTestUtils.getField(orphan("old question 2", createdAt2), "id");

        migration().run(null);

        List<ConversationThreadEntity> threads = threadRepository.findAll();
        assertThat(threads).hasSize(2);
        assertThat(threads).extracting(ConversationThreadEntity::getTitle)
                .containsExactlyInAnyOrder("old question 1", "old question 2");

        ConversationEntity reloaded1 = conversationRepository.findById(orphan1Id).orElseThrow();
        ConversationEntity reloaded2 = conversationRepository.findById(orphan2Id).orElseThrow();
        Long threadId1 = (Long) ReflectionTestUtils.getField(reloaded1, "threadId");
        Long threadId2 = (Long) ReflectionTestUtils.getField(reloaded2, "threadId");
        assertThat(threadId1).isNotNull();
        assertThat(threadId2).isNotNull();

        ConversationThreadEntity migratedThread1 = threadRepository.findById(threadId1).orElseThrow();
        assertThat(migratedThread1.getCreatedAt()).isEqualTo(createdAt1);
        assertThat(migratedThread1.getUpdatedAt()).isEqualTo(createdAt1);
    }

    @Test
    void run_twice_isIdempotent() {
        orphan("old question", Instant.parse("2026-01-01T00:00:00Z"));

        migration().run(null);
        migration().run(null);

        assertThat(threadRepository.count()).isEqualTo(1);
        assertThat(conversationRepository.findByThreadIdIsNull()).isEmpty();
    }

    @Test
    void run_noOrphans_doesNothing() {
        migration().run(null);

        assertThat(threadRepository.count()).isZero();
        assertThat(conversationRepository.count()).isZero();
    }
}
