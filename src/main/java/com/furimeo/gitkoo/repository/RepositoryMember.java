package com.furimeo.gitkoo.repository;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/** Explicit repository member grant (DESIGN.md §22). */
@Table("repository_members")
public class RepositoryMember {

    public enum Permission { READ, WRITE, ADMIN }

    @Id
    private Long id;
    private Long repositoryId;
    private Long userId;
    private String permission;

    public RepositoryMember() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getRepositoryId() { return repositoryId; }
    public void setRepositoryId(Long v) { this.repositoryId = v; }
    public Long getUserId() { return userId; }
    public void setUserId(Long v) { this.userId = v; }
    public String getPermission() { return permission; }
    public void setPermission(String v) { this.permission = v; }
}
