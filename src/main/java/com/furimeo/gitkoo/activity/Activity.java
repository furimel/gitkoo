package com.furimeo.gitkoo.activity;

import java.time.OffsetDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/** A human-readable activity event (DESIGN.md §23). */
@Table("activities")
public class Activity {

    @Id
    private Long id;
    private Long repositoryId;
    private Long actorId;
    private String type;
    private String message;
    private String payload;
    private OffsetDateTime createdAt;

    public Activity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getRepositoryId() { return repositoryId; }
    public void setRepositoryId(Long v) { this.repositoryId = v; }
    public Long getActorId() { return actorId; }
    public void setActorId(Long v) { this.actorId = v; }
    public String getType() { return type; }
    public void setType(String v) { this.type = v; }
    public String getMessage() { return message; }
    public void setMessage(String v) { this.message = v; }
    public String getPayload() { return payload; }
    public void setPayload(String v) { this.payload = v; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime v) { this.createdAt = v; }
}
