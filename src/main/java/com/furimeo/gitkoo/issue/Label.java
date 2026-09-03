package com.furimeo.gitkoo.issue;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/** A label on issues/PRs (DESIGN.md §18). */
@Table("labels")
public class Label {

    @Id
    private Long id;
    private Long repositoryId;
    private String name;
    private String color;
    private String description;

    public Label() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getRepositoryId() { return repositoryId; }
    public void setRepositoryId(Long v) { this.repositoryId = v; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public String getColor() { return color; }
    public void setColor(String v) { this.color = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
}
