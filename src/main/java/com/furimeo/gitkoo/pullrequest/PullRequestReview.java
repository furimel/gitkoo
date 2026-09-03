package com.furimeo.gitkoo.pullrequest;

import java.time.OffsetDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/** A review on a pull request (DESIGN.md §16). */
@Table("pull_request_reviews")
public class PullRequestReview {

    public enum State { COMMENT, APPROVE, REQUEST_CHANGES }

    @Id
    private Long id;
    private Long pullRequestId;
    private Long reviewerId;
    private String state;
    private String body;
    private OffsetDateTime createdAt;

    public PullRequestReview() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPullRequestId() { return pullRequestId; }
    public void setPullRequestId(Long v) { this.pullRequestId = v; }
    public Long getReviewerId() { return reviewerId; }
    public void setReviewerId(Long v) { this.reviewerId = v; }
    public String getState() { return state; }
    public void setState(String v) { this.state = v; }
    public String getBody() { return body; }
    public void setBody(String v) { this.body = v; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime v) { this.createdAt = v; }
}
