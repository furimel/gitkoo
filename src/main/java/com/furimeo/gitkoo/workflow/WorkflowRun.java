package com.furimeo.gitkoo.workflow;

import java.time.OffsetDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/** A single execution of a workflow (DESIGN.md §116). */
@Table("workflow_runs")
public class WorkflowRun {

    public enum Status { QUEUED, RUNNING, SUCCESS, FAILED, CANCELLED, TIMEOUT }

    @Id
    private Long id;
    private Long workflowId;
    private Long repositoryId;
    private String commitSha;
    private String event;
    private String ref;
    private String status;
    private Long triggeredByUserId;
    private OffsetDateTime startedAt;
    private OffsetDateTime finishedAt;

    public WorkflowRun() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getWorkflowId() { return workflowId; }
    public void setWorkflowId(Long v) { this.workflowId = v; }
    public Long getRepositoryId() { return repositoryId; }
    public void setRepositoryId(Long v) { this.repositoryId = v; }
    public String getCommitSha() { return commitSha; }
    public void setCommitSha(String v) { this.commitSha = v; }
    public String getEvent() { return event; }
    public void setEvent(String v) { this.event = v; }
    public String getRef() { return ref; }
    public void setRef(String v) { this.ref = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public Long getTriggeredByUserId() { return triggeredByUserId; }
    public void setTriggeredByUserId(Long v) { this.triggeredByUserId = v; }
    public OffsetDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(OffsetDateTime v) { this.startedAt = v; }
    public OffsetDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(OffsetDateTime v) { this.finishedAt = v; }
}
