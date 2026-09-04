package com.furimeo.gitkoo.workflow;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

/**
 * Spring Data JDBC repository for {@link WorkflowArtifact} (DESIGN.md §116).
 *
 * @see DESIGN.md §87 (no generic BaseRepository, concrete per domain)
 */
public interface WorkflowArtifactRepository extends CrudRepository<WorkflowArtifact, Long> {

    /** Lists artifacts collected for a given workflow run. */
    List<WorkflowArtifact> findByRunId(Long runId);
}
