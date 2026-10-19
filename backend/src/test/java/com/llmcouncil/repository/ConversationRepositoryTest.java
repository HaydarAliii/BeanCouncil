package com.llmcouncil.repository;

import com.llmcouncil.model.entity.ConversationEntity;
import com.llmcouncil.support.AbstractPostgresTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Integration tests for {@link ConversationRepository} against a real Postgres (Testcontainers). */
class ConversationRepositoryTest extends AbstractPostgresTest {

    @Autowired
    ConversationRepository repository;

    private ConversationEntity turn(String prompt, Long threadId, Instant createdAt) {
        ConversationEntity entity = new ConversationEntity(prompt);
        entity.setThreadId(threadId);
        ReflectionTestUtils.setField(entity, "createdAt", createdAt);
        return entity;
    }

    @Test
    void save_newEntity_assignsIdentityId() {
        ConversationEntity entity = new ConversationEntity("what's the capital of France?");

        ConversationEntity saved = repository.save(entity);

        assertThat(ReflectionTestUtils.getField(saved, "id")).isNotNull();
    }

    @Test
    void transcript_largeJson_roundTripsIntact() {
        String largeTranscript = "{\"prompt\":\"q\",\"firstOpinions\":[" + "{\"text\":\"x\"},".repeat(500) + "{\"text\":\"last\"}]}";
        ConversationEntity entity = new ConversationEntity("prompt");
        entity.setTranscript(largeTranscript);

        Long id = (Long) ReflectionTestUtils.getField(repository.save(entity), "id");
        ConversationEntity reloaded = repository.findById(id).orElseThrow();

        assertThat(reloaded.getTranscript()).isEqualTo(largeTranscript);
    }

    @Test
    void findByThreadIdOrderByCreatedAtAsc_returnsTurnsInChronologicalOrder() {
        repository.save(turn("third", 1L, Instant.parse("2026-01-03T00:00:00Z")));
        repository.save(turn("first", 1L, Instant.parse("2026-01-01T00:00:00Z")));
        repository.save(turn("second", 1L, Instant.parse("2026-01-02T00:00:00Z")));
        repository.save(turn("other thread", 2L, Instant.parse("2026-01-01T00:00:00Z")));

        List<ConversationEntity> turns = repository.findByThreadIdOrderByCreatedAtAsc(1L);

        assertThat(turns).extracting(ConversationEntity::getPrompt).containsExactly("first", "second", "third");
    }

    @Test
    void countByThreadId_countsOnlyMatchingThread() {
        repository.save(turn("a", 1L, Instant.now()));
        repository.save(turn("b", 1L, Instant.now()));
        repository.save(turn("c", 2L, Instant.now()));

        assertThat(repository.countByThreadId(1L)).isEqualTo(2);
        assertThat(repository.countByThreadId(2L)).isEqualTo(1);
        assertThat(repository.countByThreadId(999L)).isEqualTo(0);
    }

    @Test
    void findByThreadIdIsNull_returnsOnlyOrphans() {
        repository.save(turn("assigned", 1L, Instant.now()));
        repository.save(turn("orphan 1", null, Instant.now()));
        repository.save(turn("orphan 2", null, Instant.now()));

        List<ConversationEntity> orphans = repository.findByThreadIdIsNull();

        assertThat(orphans).extracting(ConversationEntity::getPrompt).containsExactlyInAnyOrder("orphan 1", "orphan 2");
    }
}
