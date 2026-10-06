package com.llmcouncil.service;

import com.llmcouncil.model.entity.ConversationEntity;
import com.llmcouncil.model.entity.ConversationThreadEntity;
import com.llmcouncil.repository.ConversationRepository;
import com.llmcouncil.repository.ConversationThreadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegacyConversationMigrationTest {

    @Mock
    ConversationRepository conversationRepository;
    @Mock
    ConversationThreadRepository threadRepository;

    LegacyConversationMigration migration;

    @BeforeEach
    void setUp() {
        migration = new LegacyConversationMigration(conversationRepository, threadRepository);
    }

    private ConversationEntity orphan(String prompt, Instant createdAt) {
        ConversationEntity entity = new ConversationEntity(prompt);
        ReflectionTestUtils.setField(entity, "createdAt", createdAt);
        return entity;
    }

    @Test
    void noOrphans_doesNothing_idempotent() {
        when(conversationRepository.findByThreadIdIsNull()).thenReturn(List.of());

        migration.run(null);

        verifyNoInteractions(threadRepository);
        verify(conversationRepository, never()).save(any());
    }

    @Test
    void oneOrphan_createsThreadWithMatchingTitleAndTimestamps_andLinksConversation() {
        Instant createdAt = Instant.parse("2026-08-03T10:00:00Z");
        ConversationEntity orphan = orphan("eski soru", createdAt);
        when(conversationRepository.findByThreadIdIsNull()).thenReturn(List.of(orphan));
        when(threadRepository.save(any(ConversationThreadEntity.class))).thenAnswer(inv -> {
            ConversationThreadEntity thread = inv.getArgument(0);
            ReflectionTestUtils.setField(thread, "id", 99L);
            return thread;
        });

        migration.run(null);

        ArgumentCaptor<ConversationThreadEntity> threadCaptor = ArgumentCaptor.forClass(ConversationThreadEntity.class);
        verify(threadRepository).save(threadCaptor.capture());
        ConversationThreadEntity savedThread = threadCaptor.getValue();
        assertThat(savedThread.getTitle()).isEqualTo("eski soru");
        assertThat(savedThread.getCreatedAt()).isEqualTo(createdAt);
        assertThat(savedThread.getUpdatedAt()).isEqualTo(createdAt);

        verify(conversationRepository).save(orphan);
        // threadId setter'ın gerçekten yeni thread'in id'siyle çağrıldığını doğrulamak için
        // ConversationEntity'nin threadId alanını reflection ile okuyoruz (public getter yok).
        assertThat(ReflectionTestUtils.getField(orphan, "threadId")).isEqualTo(99L);
    }

    @Test
    void multipleOrphans_eachGetsItsOwnThread() {
        ConversationEntity first = orphan("soru 1", Instant.parse("2026-08-03T10:00:00Z"));
        ConversationEntity second = orphan("soru 2", Instant.parse("2026-08-03T11:00:00Z"));
        when(conversationRepository.findByThreadIdIsNull()).thenReturn(List.of(first, second));
        when(threadRepository.save(any(ConversationThreadEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        migration.run(null);

        verify(threadRepository, times(2)).save(any(ConversationThreadEntity.class));
        verify(conversationRepository).save(first);
        verify(conversationRepository).save(second);
    }
}
