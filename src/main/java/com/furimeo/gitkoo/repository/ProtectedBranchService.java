package com.furimeo.gitkoo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages protected-branch rules on a repository (DESIGN.md §78).
 *
 * @see DESIGN.md §78
 */
@Service
public class ProtectedBranchService {

    private final ProtectedBranchRepository repository;

    public ProtectedBranchService(ProtectedBranchRepository repository) {
        this.repository = repository;
    }

    /**
     * Protects a branch, requiring a pull request by default.
     *
     * <p>Idempotent: protecting an already-protected branch keeps the single row and updates
     * its {@code requirePr} flag.
     */
    @Transactional
    public ProtectedBranch protect(Long repositoryId, String branchName) {
        return protect(repositoryId, branchName, true);
    }

    /**
     * Protects a branch with an explicit require-PR flag (upserts the rule).
     */
    @Transactional
    public ProtectedBranch protect(Long repositoryId, String branchName, boolean requirePr) {
        Optional<ProtectedBranch> existing = repository.findByRepositoryIdAndBranchName(repositoryId, branchName);
        ProtectedBranch rule;
        if (existing.isPresent()) {
            rule = existing.get();
            rule.setRequirePr(requirePr);
        } else {
            rule = new ProtectedBranch();
            rule.setRepositoryId(repositoryId);
            rule.setBranchName(branchName);
            rule.setRequirePr(requirePr);
        }
        return repository.save(rule);
    }

    /** Removes protection from a branch. No-op if the branch was not protected. */
    @Transactional
    public void unprotect(Long repositoryId, String branchName) {
        repository.findByRepositoryIdAndBranchName(repositoryId, branchName)
                .ifPresent(repository::delete);
    }

    /** Returns true when a branch has a protection rule. */
    public boolean isProtected(Long repositoryId, String branchName) {
        return repository.findByRepositoryIdAndBranchName(repositoryId, branchName).isPresent();
    }

    /** Lists every protected-branch rule on a repository. */
    public List<ProtectedBranch> listByRepository(Long repositoryId) {
        return repository.findByRepositoryId(repositoryId);
    }

    /**
     * Returns whether a branch requires a pull request before changes land. An unprotected
     * branch never requires a PR.
     */
    public boolean requirePr(Long repositoryId, String branchName) {
        return repository.findByRepositoryIdAndBranchName(repositoryId, branchName)
                .map(ProtectedBranch::isRequirePr)
                .orElse(false);
    }
}
