package com.furimeo.gitkoo.issue;

import java.time.OffsetDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/** A comment on an issue (DESIGN.md §19). */
@Table("issue_comments")
public class IssueComment {

    @Id
    private Long id;
    private Long issueId;
    private Long authorId;
    private String body;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public IssueComment() {}

    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    public Long getIssueId() { return issueId; }
    public void setIssueId(Long v) { this.issueId = v; }
    public Long getAuthorId() { return authorId; }
    public void setAuthorId(Long v) { this.authorId = v; }
    public String getBody() { return body; }
    public void setBody(String v) { this.body = v; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime v) { this.createdAt = v; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime v) { this.updatedAt = v; }
}
