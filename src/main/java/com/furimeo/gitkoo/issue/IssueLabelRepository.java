package com.furimeo.gitkoo.issue;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

public interface IssueLabelRepository extends CrudRepository<IssueLabel, Long> {
    List<IssueLabel> findByIssueId(Long issueId);

    /** Batch lookup, so a list of issues costs one query instead of one per row. */
    List<IssueLabel> findByIssueIdIn(java.util.Collection<Long> issueIds);
    boolean existsByIssueIdAndLabelId(Long issueId, Long labelId);
    void deleteByIssueIdAndLabelId(Long issueId, Long labelId);
}
