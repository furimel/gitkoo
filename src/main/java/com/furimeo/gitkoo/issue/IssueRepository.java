package com.furimeo.gitkoo.issue;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface IssueRepository extends CrudRepository<Issue, Long> {

    /** Per-repository issue numbering: COALESCE(MAX(number),0)+1 in a transaction (DESIGN.md §116). */
    @Query("SELECT COALESCE(MAX(number), 0) FROM issues WHERE repository_id = :repoId")
    int maxNumber(@Param("repoId") Long repoId);

    List<Issue> findByRepositoryIdOrderByNumberDesc(Long repositoryId);

    Optional<Issue> findByRepositoryIdAndNumber(Long repositoryId, Integer number);
}
