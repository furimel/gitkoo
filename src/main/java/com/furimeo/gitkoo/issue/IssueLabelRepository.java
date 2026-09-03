package com.furimeo.gitkoo.issue;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

public interface IssueLabelRepository extends CrudRepository<IssueLabel, Long> {
    List<IssueLabel> findByIssueId(Long issueId);
    boolean existsByIssueIdAndLabelId(Long issueId, Long labelId);
    void deleteByIssueIdAndLabelId(Long issueId, Long labelId);
}
