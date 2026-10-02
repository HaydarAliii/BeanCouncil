package com.llmcouncil.repository;

import com.llmcouncil.model.entity.ConversationThreadEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationThreadRepository extends JpaRepository<ConversationThreadEntity, Long> {

    List<ConversationThreadEntity> findAllByOrderByUpdatedAtDesc();
}
