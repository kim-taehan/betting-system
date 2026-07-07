package com.example.betting.ops.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** 저장된 운영 알림(RiskAlert 등). */
@Entity
@Table(name = "alert")
public class AlertEntity {

    @Id
    private String id;

    private String type;

    @Column(name = "subject_id")
    private String subjectId;

    private String message;

    @Column(name = "created_at")
    private Instant createdAt;

    protected AlertEntity() {
    }

    public AlertEntity(String id, String type, String subjectId, String message, Instant createdAt) {
        this.id = id;
        this.type = type;
        this.subjectId = subjectId;
        this.message = message;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public String getMessage() {
        return message;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
