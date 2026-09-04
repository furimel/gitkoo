package com.furimeo.gitkoo.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration test for {@link ProtectedBranchService}.
 *
 * <p>Uses the real database (SQLite) per the testing strategy; the migration runner creates
 * the {@code protected_branches} table at startup (DESIGN.md §116). Protected-branch rows
 * reference a real repository because SQLite enforces the foreign key
 * (DESIGN.md §116: {@code PRAGMA foreign_keys = ON}).
 */
@SpringBootTest
@Transactional
class ProtectedBranchServiceTest {

    @Autowired
    private ProtectedBranchService protectedBranchService;

    @Autowired
    private RepositoryService repositoryService;

    @Test
    void protectAndUnprotectBranch() {
        Repository repo = createRepo("pb-protect");
        assertThat(protectedBranchService.isProtected(repo.getId(), "main")).isFalse();
        assertThat(protectedBranchService.requirePr(repo.getId(), "main")).isFalse();

        protectedBranchService.protect(repo.getId(), "main");
        assertThat(protectedBranchService.isProtected(repo.getId(), "main")).isTrue();
        assertThat(protectedBranchService.requirePr(repo.getId(), "main")).isTrue();
        assertThat(protectedBranchService.listByRepository(repo.getId())).hasSize(1);

        protectedBranchService.unprotect(repo.getId(), "main");
        assertThat(protectedBranchService.isProtected(repo.getId(), "main")).isFalse();
        assertThat(protectedBranchService.requirePr(repo.getId(), "main")).isFalse();
        assertThat(protectedBranchService.listByRepository(repo.getId())).isEmpty();
    }

    @Test
    void protectIsIdempotentAndUpdatesRequirePr() {
        Repository repo = createRepo("pb-upsert");
        protectedBranchService.protect(repo.getId(), "release", true);
        protectedBranchService.protect(repo.getId(), "release", false);

        assertThat(protectedBranchService.isProtected(repo.getId(), "release")).isTrue();
        assertThat(protectedBranchService.requirePr(repo.getId(), "release")).isFalse();
        // Upsert keeps a single row.
        assertThat(protectedBranchService.listByRepository(repo.getId())).hasSize(1);
    }

    @Test
    void unprotectIsNoopWhenNotProtected() {
        Repository repo = createRepo("pb-noop");
        protectedBranchService.unprotect(repo.getId(), "absent");
        assertThat(protectedBranchService.isProtected(repo.getId(), "absent")).isFalse();
    }

    private Repository createRepo(String name) {
        return repositoryService.create(
                Repository.OwnerType.USER.name(), 777L, name, null, "PUBLIC", "main");
    }
}
