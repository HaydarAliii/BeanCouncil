package com.llmcouncil.repository;

import com.llmcouncil.model.entity.ConversationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationRepository extends JpaRepository<ConversationEntity, Long> {

    List<ConversationEntity> findByThreadIdOrderByCreatedAtAsc(Long threadId);

    long countByThreadId(Long threadId);

    /** Follow-up özelliğinden önce kaydedilmiş, henüz bir thread'e taşınmamış turlar. */
    List<ConversationEntity> findByThreadIdIsNull();
}
