package com.furimeo.gitkoo.issue;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

public interface IssueCommentRepository extends CrudRepository<IssueComment, Long> {
    List<IssueComment> findByIssueIdOrderByCreatedAtAsc(Long issueId);
}
