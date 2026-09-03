package com.furimeo.gitkoo.workflow;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

public interface WorkflowRunRepository extends CrudRepository<WorkflowRun, Long> {
    List<WorkflowRun> findByRepositoryIdOrderByIdDesc(Long repositoryId);
}
