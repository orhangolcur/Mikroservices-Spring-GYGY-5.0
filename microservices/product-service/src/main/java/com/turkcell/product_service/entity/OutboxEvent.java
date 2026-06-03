package com.turkcell.product_service.entity;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "outbox")
public class OutboxEvent {

    @Id
    private UUID id;
    private String aggregateType; // Product
    private String aggregateId; // Product ID
    private String eventType; // TestEvent
    @Column(columnDefinition = "TEXT")
    private String payload; // JSON
    private String errorMessage; // Hata varsa buraya kaydedelim
    private int retryCount; // Kaç kere denendi
    
    private Instant createdAt; // Şu tarihte sıraya eklendi
    private Instant processedAt; // Şu tarihte kafaya gönderildi

    @Enumerated(EnumType.STRING) // numara olarak mı saklayalım yoksa string olarak mı saklayalım
    private OutboxStatus status; // PENDING, SENT, FAILED
}
