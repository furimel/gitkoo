package com.furimeo.gitkoo.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration test for repository creation and Git storage (DESIGN.md §5, §94).
 *
 * <p>Uses the real Git binary (no mock) per the testing strategy \u2014 Git functionality
 * should be tested with a real repository (DESIGN.md §95).
 */
@SpringBootTest
@Transactional
class RepositoryServiceTest {

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private RepositoryRepository repositoryRepository;

    @Test
    void createRepositoryInitializesBareGitRepo() {
        Repository repo = repositoryService.create(
                Repository.OwnerType.USER.name(), 1L, "test-repo",
                "A test repository", "PUBLIC", "main");

        assertThat(repo.getId()).isNotNull();
        assertThat(repo.getName()).isEqualTo("test-repo");
        assertThat(repo.getStoragePath()).isNotBlank();
        assertThat(repo.getVisibility()).isEqualTo("PUBLIC");
        assertThat(repo.getDefaultBranch()).isEqualTo("main");

        Path storagePath = Path.of(repo.getStoragePath());
        assertThat(Files.isDirectory(storagePath)).as("bare repo directory exists").isTrue();
        assertThat(Files.exists(storagePath.resolve("HEAD"))).as("bare repo has HEAD").isTrue();
        assertThat(Files.isDirectory(storagePath.resolve("objects"))).as("bare repo has objects").isTrue();
        assertThat(Files.isDirectory(storagePath.resolve("refs"))).as("bare repo has refs").isTrue();
    }

    @Test
    void createRepositoryRejectsDuplicateName() {
        repositoryService.create(Repository.OwnerType.USER.name(), 2L, "dup", null, "PUBLIC", "main");
        assertThatThrownBy(() -> repositoryService.create(
                Repository.OwnerType.USER.name(), 2L, "dup", null, "PUBLIC", "main"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void resolveStoragePathIsIdBasedAndAbsolute() {
        Path path = repositoryService.resolveStoragePath(42L);
        assertThat(path.isAbsolute()).isTrue();
        assertThat(path.toString()).endsWith("42.git");
        assertThat(path.toString()).contains("git").contains("repositories");
    }

    @Test
    void validateNameAcceptsValidNames() {
        RepositoryService.validateName("pump");
        RepositoryService.validateName("my-repo");
        RepositoryService.validateName("my_repo.v2");
        RepositoryService.validateName("A");
    }

    @Test
    void validateNameRejectsInvalidNames() {
        assertThatThrownBy(() -> RepositoryService.validateName(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RepositoryService.validateName("")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RepositoryService.validateName("-leading-hyphen")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RepositoryService.validateName("has space")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RepositoryService.validateName("has/slash")).isInstanceOf(IllegalArgumentException.class);
    }
}
