package com.furimeo.gitkoo.activity;

import java.time.OffsetDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/** A security-oriented audit event (see docs/). */
@Table("audit_events")
public class AuditEvent {

    @Id
    private Long id;
    private Long actorId;
    private String action;
    private String targetType;
    private Long targetId;
    private String ip;
    private OffsetDateTime createdAt;

    public AuditEvent() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getActorId() { return actorId; }
    public void setActorId(Long v) { this.actorId = v; }
    public String getAction() { return action; }
    public void setAction(String v) { this.action = v; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String v) { this.targetType = v; }
    public Long getTargetId() { return targetId; }
    public void setTargetId(Long v) { this.targetId = v; }
    public String getIp() { return ip; }
    public void setIp(String v) { this.ip = v; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime v) { this.createdAt = v; }
}
