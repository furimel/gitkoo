package com.furimeo.gitkoo.workflow;

import java.time.OffsetDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * An artifact collected from a workflow run (DESIGN.md §116).
 *
 * <p>When a workflow declares {@code artifact <glob>}, matching files in the
 * workspace are copied into {@code <data>/artifacts/{runId}/...} and a row is
 * recorded here so the UI can list and serve them.
 *
 * @see DESIGN.md §116
 */
@Table("workflow_artifacts")
public class WorkflowArtifact {

    @Id
    private Long id;
    private Long runId;
    private String name;
    private String filePath;
    private Long size;
    private OffsetDateTime createdAt;
    private OffsetDateTime expiresAt;

    public WorkflowArtifact() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRunId() {
        return runId;
    }

    public void setRunId(Long runId) {
        this.runId = runId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(OffsetDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}
