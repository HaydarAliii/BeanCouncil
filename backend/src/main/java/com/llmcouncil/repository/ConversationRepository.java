package com.llmcouncil.repository;

import com.llmcouncil.model.entity.ConversationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationRepository extends JpaRepository<ConversationEntity, Long> {

    List<ConversationEntity> findByThreadIdOrderByCreatedAtAsc(Long threadId);

    long countByThreadId(Long threadId);

    /** Turns saved before the follow-up feature existed, not yet migrated to a thread. */
    List<ConversationEntity> findByThreadIdIsNull();
}
