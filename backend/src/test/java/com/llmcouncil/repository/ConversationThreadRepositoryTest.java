package com.llmcouncil.repository;

import com.llmcouncil.model.entity.ConversationThreadEntity;
import com.llmcouncil.support.AbstractPostgresTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Integration tests for {@link ConversationThreadRepository} against a real Postgres (Testcontainers). */
class ConversationThreadRepositoryTest extends AbstractPostgresTest {

    @Autowired
    ConversationThreadRepository repository;

    @Test
    void save_newThread_assignsIdentityId() {
        ConversationThreadEntity saved = repository.save(new ConversationThreadEntity("first question"));

        assertThat(saved.getId()).isNotNull();
    }

    @Test
    void findAllByOrderByUpdatedAtDesc_ordersByUpdatedAtDescending() {
        ConversationThreadEntity older = new ConversationThreadEntity("older thread");
        older.setUpdatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        ConversationThreadEntity newer = new ConversationThreadEntity("newer thread");
        newer.setUpdatedAt(Instant.parse("2026-01-02T00:00:00Z"));
        repository.save(older);
        repository.save(newer);

        List<ConversationThreadEntity> threads = repository.findAllByOrderByUpdatedAtDesc();

        assertThat(threads).extracting(ConversationThreadEntity::getTitle).containsExactly("newer thread", "older thread");
    }

    @Test
    void touch_thenReindex_movesThreadToFront() {
        ConversationThreadEntity first = new ConversationThreadEntity("first thread");
        first.setUpdatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        ConversationThreadEntity second = new ConversationThreadEntity("second thread");
        second.setUpdatedAt(Instant.parse("2026-01-02T00:00:00Z"));
        repository.save(first);
        repository.save(second);

        first.touch();
        repository.save(first);

        List<ConversationThreadEntity> threads = repository.findAllByOrderByUpdatedAtDesc();
        assertThat(threads.get(0).getTitle()).isEqualTo("first thread");
    }
}
