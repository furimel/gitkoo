package com.furimeo.gitkoo.repository;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * A protected branch rule on a repository.
 *
 * <p>When a branch is protected, direct pushes are discouraged and a pull request may be
 * required before changes land (DESIGN.md §78). The database stores only this metadata;
 * Git state still lives on the filesystem (DESIGN.md §5).
 *
 * @see DESIGN.md §78
 */
@Table("protected_branches")
public class ProtectedBranch {

    @Id
    private Long id;
    private Long repositoryId;
    private String branchName;
    private boolean requirePr;

    public ProtectedBranch() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(Long repositoryId) {
        this.repositoryId = repositoryId;
    }

    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    public boolean isRequirePr() {
        return requirePr;
    }

    public void setRequirePr(boolean requirePr) {
        this.requirePr = requirePr;
    }
}
