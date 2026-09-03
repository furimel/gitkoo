package com.furimeo.gitkoo.pullrequest;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

public interface PullRequestReviewRepository extends CrudRepository<PullRequestReview, Long> {
    List<PullRequestReview> findByPullRequestIdOrderByCreatedAtAsc(Long pullRequestId);
}
