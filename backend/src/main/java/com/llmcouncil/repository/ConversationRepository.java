package com.llmcouncil.repository;

import com.llmcouncil.model.entity.ConversationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationRepository extends JpaRepository<ConversationEntity, Long> {

    List<ConversationEntity> findAllByOrderByCreatedAtDesc();
}
