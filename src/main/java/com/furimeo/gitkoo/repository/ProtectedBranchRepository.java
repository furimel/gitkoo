package com.furimeo.gitkoo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

/**
 * Spring Data JDBC repository for {@link ProtectedBranch}.
 *
 * @see DESIGN.md §87 (no generic BaseRepository, concrete per domain)
 */
public interface ProtectedBranchRepository extends CrudRepository<ProtectedBranch, Long> {

    /** Lists every protected-branch rule on a repository. */
    List<ProtectedBranch> findByRepositoryId(Long repositoryId);

    /** Finds a specific protected-branch rule, if any. */
    Optional<ProtectedBranch> findByRepositoryIdAndBranchName(Long repositoryId, String branchName);
}
