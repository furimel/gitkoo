package com.furimeo.gitkoo.pullrequest;

import java.time.OffsetDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/** A pull request (DESIGN.md §15). */
@Table("pull_requests")
public class PullRequest {

    public enum Status { OPEN, MERGED, CLOSED, DRAFT }

    @Id
    private Long id;
    private Long repositoryId;
    private Integer number;
    private String title;
    private String body;
    private Long authorId;
    private String sourceBranch;
    private Long sourceRepositoryId;
    private String targetBranch;
    private String status;
    private String mergeCommitSha;
    private Long assigneeId;
    private Long milestoneId;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime mergedAt;
    private OffsetDateTime closedAt;

    public PullRequest() {}

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
    public String getSourceBranch() { return sourceBranch; }
    public void setSourceBranch(String v) { this.sourceBranch = v; }
    public Long getSourceRepositoryId() { return sourceRepositoryId; }
    public void setSourceRepositoryId(Long v) { this.sourceRepositoryId = v; }
    public String getTargetBranch() { return targetBranch; }
    public void setTargetBranch(String v) { this.targetBranch = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public String getMergeCommitSha() { return mergeCommitSha; }
    public void setMergeCommitSha(String v) { this.mergeCommitSha = v; }
    public Long getAssigneeId() { return assigneeId; }
    public void setAssigneeId(Long v) { this.assigneeId = v; }
    public Long getMilestoneId() { return milestoneId; }
    public void setMilestoneId(Long v) { this.milestoneId = v; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime v) { this.createdAt = v; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime v) { this.updatedAt = v; }
    public OffsetDateTime getMergedAt() { return mergedAt; }
    public void setMergedAt(OffsetDateTime v) { this.mergedAt = v; }
    public OffsetDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(OffsetDateTime v) { this.closedAt = v; }
}
