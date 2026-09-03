package com.furimeo.gitkoo.issue;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

public interface MilestoneRepository extends CrudRepository<Milestone, Long> {
    List<Milestone> findByRepositoryId(Long repositoryId);
}
