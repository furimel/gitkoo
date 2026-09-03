package com.furimeo.gitkoo.activity;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

public interface ActivityRepository extends CrudRepository<Activity, Long> {
    List<Activity> findByRepositoryIdOrderByCreatedAtDesc(Long repositoryId);
    List<Activity> findTop20ByOrderByCreatedAtDesc();
}
