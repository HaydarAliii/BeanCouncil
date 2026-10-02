package com.llmcouncil.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Bir "konuşma" (thread) — içinde birden fazla {@link ConversationEntity} turu olabilir
 * (follow-up sorular). {@code title} thread'in ilk sorusudur, değişmez.
 */
@Entity
@Table(name = "conversation_threads")
public class ConversationThreadEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String title;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    protected ConversationThreadEntity() {
    }

    public ConversationThreadEntity(String title) {
        this.title = title;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    /** Thread'e yeni bir tur eklendiğinde son aktivite zamanını günceller. */
    public void touch() {
        this.updatedAt = Instant.now();
    }
}
