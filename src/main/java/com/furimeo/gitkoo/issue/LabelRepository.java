package com.furimeo.gitkoo.issue;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

public interface LabelRepository extends CrudRepository<Label, Long> {
    List<Label> findByRepositoryId(Long repositoryId);
    Optional<Label> findByRepositoryIdAndName(Long repositoryId, String name);
    boolean existsByRepositoryIdAndName(Long repositoryId, String name);
}
