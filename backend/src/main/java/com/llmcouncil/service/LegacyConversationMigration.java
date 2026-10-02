package com.llmcouncil.service;

import com.llmcouncil.model.entity.ConversationEntity;
import com.llmcouncil.model.entity.ConversationThreadEntity;
import com.llmcouncil.repository.ConversationRepository;
import com.llmcouncil.repository.ConversationThreadRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Follow-up/thread özelliği eklenmeden önce kaydedilmiş konuşmaların {@code threadId}'si null'dır.
 * Uygulama her başladığında bu turları kendi tek-turluk thread'lerine taşır (idempotent —
 * bir kere taşınan satır bir daha {@code findByThreadIdIsNull()} ile dönmez).
 */
@Component
public class LegacyConversationMigration implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LegacyConversationMigration.class);

    private final ConversationRepository conversationRepository;
    private final ConversationThreadRepository threadRepository;

    public LegacyConversationMigration(ConversationRepository conversationRepository,
                                        ConversationThreadRepository threadRepository) {
        this.conversationRepository = conversationRepository;
        this.threadRepository = threadRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<ConversationEntity> orphans = conversationRepository.findByThreadIdIsNull();
        if (orphans.isEmpty()) {
            return;
        }
        for (ConversationEntity conversation : orphans) {
            ConversationThreadEntity thread = new ConversationThreadEntity(conversation.getPrompt());
            thread.setCreatedAt(conversation.getCreatedAt());
            thread.setUpdatedAt(conversation.getCreatedAt());
            thread = threadRepository.save(thread);

            conversation.setThreadId(thread.getId());
            conversationRepository.save(conversation);
        }
        log.info("Migrated {} legacy conversation(s) into their own threads", orphans.size());
    }
}
