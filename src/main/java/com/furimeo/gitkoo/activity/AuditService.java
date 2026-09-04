package com.furimeo.gitkoo.activity;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

/** Records and lists security-oriented audit events (see docs/). */
@Service
public class AuditService {

    private final AuditEventRepository repository;

    public AuditService(AuditEventRepository repository) {
        this.repository = repository;
    }

    public AuditEvent record(Long actorId, String action, String targetType, Long targetId, String ip) {
        AuditEvent event = new AuditEvent();
        event.setActorId(actorId);
        event.setAction(action);
        event.setTargetType(targetType);
        event.setTargetId(targetId);
        event.setIp(ip);
        event.setCreatedAt(OffsetDateTime.now());
        return repository.save(event);
    }

    public List<AuditEvent> listRecent() {
        return repository.findTop50ByOrderByCreatedAtDesc();
    }
}
