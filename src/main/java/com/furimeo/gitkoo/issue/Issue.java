package com.furimeo.gitkoo.issue;

import java.time.OffsetDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * A repository issue (DESIGN.md §18).
 *
 * @see DESIGN.md §18, §116
 */
@Table("issues")
public class Issue {

    public enum Status { OPEN, CLOSED }

    @Id
    private Long id;
    private Long repositoryId;
    private Integer number;
    private String title;
    private String body;
    private Long authorId;
    private Long assigneeId;
    private Long milestoneId;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime closedAt;

    public Issue() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getRepositoryId() { return repositoryId; }
    public void setRepositoryId(Long v) { this.repositoryId = v; }
    public Integer getNumber() { return number; }
    public void setNumber(Integer v) { this.number = v; }
    public String getTitle() { return title; }
    public void setTitle(String v) { this.title = v; }
    public String getBody() { return body; }
    public void setBody(String v) { this.body = v; }
    public Long getAuthorId() { return authorId; }
    public void setAuthorId(Long v) { this.authorId = v; }
    public Long getAssigneeId() { return assigneeId; }
    public void setAssigneeId(Long v) { this.assigneeId = v; }
    public Long getMilestoneId() { return milestoneId; }
    public void setMilestoneId(Long v) { this.milestoneId = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime v) { this.createdAt = v; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime v) { this.updatedAt = v; }
    public OffsetDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(OffsetDateTime v) { this.closedAt = v; }
}
