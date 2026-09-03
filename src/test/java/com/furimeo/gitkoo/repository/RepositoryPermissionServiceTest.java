package com.furimeo.gitkoo.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.furimeo.gitkoo.auth.User;
import com.furimeo.gitkoo.auth.UserRepository;
import com.furimeo.gitkoo.auth.UserService;
import com.furimeo.gitkoo.team.TeamService;

/**
 * Integration test for the repository permission model (DESIGN.md §22, §94).
 */
@SpringBootTest
@Transactional
class RepositoryPermissionServiceTest {

    @Autowired private RepositoryPermissionService permissionService;
    @Autowired private RepositoryService repositoryService;
    @Autowired private UserRepository userRepository;
    @Autowired private UserService userService;
    @Autowired private TeamService teamService;

    @Test
    void ownerGetsAdmin() {
        User owner = createUser("owner1");
        Repository repo = createRepo(owner, "perm-test-owner");
        assertThat(permissionService.permission(owner, repo)).isEqualTo(RepositoryPermissionService.Permission.ADMIN);
    }

    @Test
    void publicRepoGivesReadToAnonymous() {
        User owner = createUser("owner2");
        Repository repo = createRepo(owner, "perm-test-public");
        assertThat(permissionService.permission(null, repo)).isEqualTo(RepositoryPermissionService.Permission.READ);
    }

    @Test
    void privateRepoGivesNoneToNonMember() {
        User owner = createUser("owner3");
        User other = createUser("other3");
        Repository repo = createPrivateRepo(owner, "perm-test-private");
        assertThat(permissionService.permission(other, repo)).isEqualTo(RepositoryPermissionService.Permission.NONE);
    }

    @Test
    void privateRepoGivesReadToOwner() {
        User owner = createUser("owner4");
        Repository repo = createPrivateRepo(owner, "perm-test-private2");
        assertThat(permissionService.permission(owner, repo)).isEqualTo(RepositoryPermissionService.Permission.ADMIN);
    }

    @Test
    void hasPermissionChecks() {
        User owner = createUser("owner5");
        Repository repo = createRepo(owner, "perm-test-has");
        assertThat(permissionService.hasPermission(owner, repo, RepositoryPermissionService.Permission.ADMIN)).isTrue();
        assertThat(permissionService.hasPermission(owner, repo, RepositoryPermissionService.Permission.READ)).isTrue();
    }

    private User createUser(String username) {
        return userService.createUser(username, username + "@test.com", "password123");
    }

    private Repository createRepo(User owner, String name) {
        return repositoryService.create(Repository.OwnerType.USER.name(), owner.getId(),
                name, null, "PUBLIC", "main");
    }

    private Repository createPrivateRepo(User owner, String name) {
        return repositoryService.create(Repository.OwnerType.USER.name(), owner.getId(),
                name, null, "PRIVATE", "main");
    }
}
