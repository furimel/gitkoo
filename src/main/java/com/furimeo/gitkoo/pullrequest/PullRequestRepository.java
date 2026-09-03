package com.furimeo.gitkoo.pullrequest;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface PullRequestRepository extends CrudRepository<PullRequest, Long> {

    @Query("SELECT COALESCE(MAX(number), 0) FROM pull_requests WHERE repository_id = :repoId")
    int maxNumber(@Param("repoId") Long repoId);

    List<PullRequest> findByRepositoryIdOrderByNumberDesc(Long repositoryId);

    Optional<PullRequest> findByRepositoryIdAndNumber(Long repositoryId, Integer number);
}
