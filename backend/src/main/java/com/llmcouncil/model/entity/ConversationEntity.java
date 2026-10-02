package com.llmcouncil.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "conversations")
public class ConversationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String prompt;

    @Column(columnDefinition = "TEXT")
    private String finalAnswer;

    @Column(columnDefinition = "TEXT")
    private String transcript;

    /** Bu turun ait olduğu konuşma (thread). Eski (follow-up özelliğinden önceki) kayıtlarda
     * başlangıçta null olabilir — {@code LegacyConversationMigration} bunları tek turluk kendi
     * thread'lerine taşır. */
    @Column
    private Long threadId;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected ConversationEntity() {
    }

    public ConversationEntity(String prompt) {
        this.prompt = prompt;
    }

    public Long getId() {
        return id;
    }

    public String getPrompt() {
        return prompt;
    }

    public String getFinalAnswer() {
        return finalAnswer;
    }

    public void setFinalAnswer(String finalAnswer) {
        this.finalAnswer = finalAnswer;
    }

    public String getTranscript() {
        return transcript;
    }

    public void setTranscript(String transcript) {
        this.transcript = transcript;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Long getThreadId() {
        return threadId;
    }

    public void setThreadId(Long threadId) {
        this.threadId = threadId;
    }
}
