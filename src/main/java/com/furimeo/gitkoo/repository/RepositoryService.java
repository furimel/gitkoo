package com.furimeo.gitkoo.repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.furimeo.gitkoo.activity.ActivityService;
import com.furimeo.gitkoo.activity.AuditService;
import com.furimeo.gitkoo.auth.UserService;
import com.furimeo.gitkoo.config.GitKooProperties;
import com.furimeo.gitkoo.git.GitService;

/**
 * Creates repositories and resolves them by owner/name (DESIGN.md §5, §69, §70).
 *
 * <p>Creating a repository does two things: inserts a metadata row (with a generated id)
 * and initializes a bare Git repository at the ID-based storage path. The storage path is
 * ID-based (not name-based) so renaming a repository does not move files on disk (§71).
 *
 * @see DESIGN.md §5, §69, §70, §116
 */
@Service
public class RepositoryService {

    /** Repository name constraints: alphanumerics, hyphens, underscores, dots; 1–100 chars. */
    static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._-]{0,99}$");

    private static final String REPO_SUBDIR = "repositories";

    private final RepositoryRepository repositoryRepository;
    private final GitService gitService;
    private final GitKooProperties properties;
    private final UserService userService;
    private final ActivityService activityService;
    private final AuditService auditService;

    public RepositoryService(RepositoryRepository repositoryRepository, GitService gitService,
                             GitKooProperties properties, UserService userService,
                             ActivityService activityService, AuditService auditService) {
        this.repositoryRepository = repositoryRepository;
        this.gitService = gitService;
        this.properties = properties;
        this.userService = userService;
        this.activityService = activityService;
        this.auditService = auditService;
    }

    public Optional<Repository> findById(Long id) {
        return repositoryRepository.findById(id);
    }

    /**
     * Finds a repository by its owner and name, e.g. {@code minh/pump}.
     *
     * @param ownerType {@code USER} or {@code TEAM}
     */
    public Optional<Repository> findByOwnerAndName(String ownerType, Long ownerId, String name) {
        return repositoryRepository.findByOwnerTypeAndOwnerIdAndName(ownerType, ownerId, name);
    }

    /**
     * Finds a USER-owned repository by owner username and repo name, for SSH/git
     * transport where we only have the username string (DESIGN.md §8).
     */
    public Optional<Repository> findByOwnerUsernameAndName(String username, String name) {
        var user = userService.findByUsername(username);
        if (user.isEmpty()) return Optional.empty();
        return repositoryRepository.findByOwnerTypeAndOwnerIdAndName(
                Repository.OwnerType.USER.name(), user.get().getId(), name);
    }

    public List<Repository> findByOwner(String ownerType, Long ownerId) {
        return repositoryRepository.findByOwnerTypeAndOwnerId(ownerType, ownerId);
    }

    /**
     * Creates a new repository: saves the metadata row, then initializes the bare Git repo.
     *
     * @throws IllegalArgumentException if the name is invalid or already taken by the owner
     */
    public Repository create(String ownerType, Long ownerId, String name, String description,
                             String visibility, String defaultBranch) {
        validateName(name);
        if (findByOwnerAndName(ownerType, ownerId, name).isPresent()) {
            throw new IllegalArgumentException("A repository named '" + name + "' already exists for this owner");
        }

        Repository repo = new Repository();
        repo.setOwnerType(ownerType);
        repo.setOwnerId(ownerId);
        repo.setName(name);
        repo.setDescription(description);
        repo.setVisibility(visibility != null ? visibility : Repository.Visibility.PUBLIC.name());
        repo.setDefaultBranch(defaultBranch != null ? defaultBranch : "main");
        repo.setArchived(false);
        OffsetDateTime now = OffsetDateTime.now();
        repo.setCreatedAt(now);
        repo.setUpdatedAt(now);
        repo = repositoryRepository.save(repo);

        // Now that we have an ID, set the storage path and init the bare repo.
        Path storagePath = resolveStoragePath(repo.getId());
        repo.setStoragePath(storagePath.toString());
        repositoryRepository.save(repo);

        try {
            Files.createDirectories(storagePath.getParent());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create repository directory " + storagePath.getParent(), e);
        }
        gitService.initBare(storagePath, repo.getDefaultBranch());

        activityService.record(repo.getId(), ownerId, "REPOSITORY_CREATED",
                "created repository " + ownerType.toLowerCase() + ":" + name);
        auditService.record(ownerId, "REPOSITORY_CREATED", "repository", repo.getId(), null);

        return repo;
    }

    /**
     * Forks a repository into another account: a new metadata row plus a real
     * {@code git clone --bare} of the source, so the fork has the full history and
     * can be cloned, pushed to, and opened pull requests from immediately.
     *
     * <p>The caller is responsible for checking that the actor may read the source;
     * this method only enforces the naming rules.
     *
     * @param sourceId the repository being forked
     * @param ownerType owner type of the new fork
     * @param ownerId owner of the new fork
     * @return the newly created fork
     * @throws IllegalArgumentException if the owner already has a repository by that name
     */
    public Repository fork(Long sourceId, String ownerType, Long ownerId) {
        Repository source = repositoryRepository.findById(sourceId)
                .orElseThrow(() -> new IllegalArgumentException("Repository not found"));

        if (findByOwnerAndName(ownerType, ownerId, source.getName()).isPresent()) {
            throw new IllegalArgumentException(
                    "You already have a repository named '" + source.getName() + "'");
        }

        Repository fork = new Repository();
        fork.setOwnerType(ownerType);
        fork.setOwnerId(ownerId);
        fork.setName(source.getName());
        fork.setDescription(source.getDescription());
        // A fork of a private repository stays private; a fork of a public one is public.
        fork.setVisibility(source.getVisibility());
        fork.setDefaultBranch(source.getDefaultBranch());
        fork.setForkOfId(source.getId());
        fork.setArchived(false);
        OffsetDateTime now = OffsetDateTime.now();
        fork.setCreatedAt(now);
        fork.setUpdatedAt(now);
        fork = repositoryRepository.save(fork);

        Path storagePath = resolveStoragePath(fork.getId());
        fork.setStoragePath(storagePath.toString());
        repositoryRepository.save(fork);

        try {
            Files.createDirectories(storagePath.getParent());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create fork directory " + storagePath.getParent(), e);
        }

        // clone --bare runs from the parent, since the target must not exist yet.
        GitService.GitResult result = gitService.run(storagePath.getParent(),
                "clone", "--bare", source.getStoragePath(), storagePath.toString());
        if (!result.success()) {
            repositoryRepository.delete(fork);
            throw new IllegalStateException("Failed to clone " + source.getName() + ": " + result.stderr());
        }

        activityService.record(fork.getId(), ownerId, "REPOSITORY_FORKED",
                "forked repository " + source.getName());
        auditService.record(ownerId, "REPOSITORY_FORKED", "repository", fork.getId(), null);

        return fork;
    }

    /**
     * Returns the absolute filesystem path for a repository's bare Git data:
     * {@code <data>/git/repositories/{id}.git} (DESIGN.md §70).
     */
    public Path resolveStoragePath(Long repositoryId) {
        return Path.of(properties.getData()).toAbsolutePath().normalize()
                .resolve("git").resolve(REPO_SUBDIR).resolve(repositoryId + ".git");
    }

    static void validateName(String name) {
        if (name == null || !NAME_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException(
                    "Repository name must be 1-100 characters of letters, digits, dots, hyphens, or underscores");
        }
    }
}
