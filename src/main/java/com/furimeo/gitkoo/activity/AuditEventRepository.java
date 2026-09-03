package com.furimeo.gitkoo.activity;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

public interface AuditEventRepository extends CrudRepository<AuditEvent, Long> {
    List<AuditEvent> findTop50ByOrderByCreatedAtDesc();
}
