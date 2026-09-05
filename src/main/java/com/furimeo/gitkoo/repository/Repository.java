package com.furimeo.gitkoo.repository;

import java.time.OffsetDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * A GitKoo repository.
 *
 * <p>The owner is polymorphic: {@code ownerType} is {@code USER} or {@code TEAM} and
 * {@code ownerId} references either a user or a team (no FK \u2014 app-enforced). The actual
 * Git data lives on the filesystem at {@code storagePath}; the database only stores this
 * metadata (DESIGN.md §5, §116).
 *
 * @see DESIGN.md §11, §116
 */
@Table("repositories")
public class Repository {

    /** Owner type: a repository can belong to a user or a team. */
    public enum OwnerType {
        USER,
        TEAM
    }

    /** Visibility level. MVP uses PUBLIC and PRIVATE; INTERNAL is reserved. */
    public enum Visibility {
        PUBLIC,
        PRIVATE,
        INTERNAL
    }

    @Id
    private Long id;

    private String ownerType;
    private Long ownerId;
    private String name;
    private String description;
    private String visibility;
    private String defaultBranch;
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String storagePath;
    private boolean archived;
    /** The repository this one was forked from, or null for an original. */
    private Long forkOfId;
    private String homepage;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public Repository() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOwnerType() {
        return ownerType;
    }

    public void setOwnerType(String ownerType) {
        this.ownerType = ownerType;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public Long getForkOfId() {
        return forkOfId;
    }

    public void setForkOfId(Long forkOfId) {
        this.forkOfId = forkOfId;
    }

    public String getHomepage() {
        return homepage;
    }

    public void setHomepage(String homepage) {
        this.homepage = homepage;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public String getDefaultBranch() {
        return defaultBranch;
    }

    public void setDefaultBranch(String defaultBranch) {
        this.defaultBranch = defaultBranch;
    }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
