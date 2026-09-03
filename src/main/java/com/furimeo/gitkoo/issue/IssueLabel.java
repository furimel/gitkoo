package com.furimeo.gitkoo.issue;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/** Association between an issue and a label (DESIGN.md §18). */
@Table("issue_labels")
public class IssueLabel {

    @Id
    private Long id;
    private Long issueId;
    private Long labelId;

    public IssueLabel() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getIssueId() { return issueId; }
    public void setIssueId(Long v) { this.issueId = v; }
    public Long getLabelId() { return labelId; }
    public void setLabelId(Long v) { this.labelId = v; }
}
