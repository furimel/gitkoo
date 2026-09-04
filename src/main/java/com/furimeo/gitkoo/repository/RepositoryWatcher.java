package com.furimeo.gitkoo.repository;

import java.time.OffsetDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/** One user watching one repository. */
@Table("repository_watchers")
public class RepositoryWatcher {

    @Id
    private Long id;

    private Long repositoryId;
    private Long userId;
    private OffsetDateTime createdAt;

    public RepositoryWatcher() {
    }

    public RepositoryWatcher(Long repositoryId, Long userId, OffsetDateTime createdAt) {
        this.repositoryId = repositoryId;
        this.userId = userId;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(Long repositoryId) {
        this.repositoryId = repositoryId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
