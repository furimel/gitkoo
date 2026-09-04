package com.furimeo.gitkoo.repository;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.furimeo.gitkoo.auth.User;
import com.furimeo.gitkoo.auth.UserService;
import com.furimeo.gitkoo.git.GitService;
import com.furimeo.gitkoo.git.GitService.CommitInfo;
import com.furimeo.gitkoo.git.GitService.TreeEntry;
import com.furimeo.gitkoo.repository.RepositoryPermissionService;
import com.furimeo.gitkoo.repository.RepositoryPermissionService.Permission;

/**
 * Repository creation and code browsing (DESIGN.md §12, §69).
 *
 * <p>MVP supports USER-owned repositories only; team ownership lands in a later phase.
 * Routes use {@code /{username}/{name}} so URLs are human-friendly while the on-disk
 * storage stays ID-based (DESIGN.md §70).
 */
@Controller
@RequestMapping
public class RepositoryController {

    private final RepositoryService repositoryService;
    private final RepositoryRepository repositoryRepository;
    private final UserService userService;
    private final GitService gitService;
    private final RepositoryPermissionService permissionService;

    public RepositoryController(RepositoryService repositoryService, RepositoryRepository repositoryRepository,
                                UserService userService, GitService gitService,
                                RepositoryPermissionService permissionService) {
        this.repositoryService = repositoryService;
        this.repositoryRepository = repositoryRepository;
        this.userService = userService;
        this.gitService = gitService;
        this.permissionService = permissionService;
    }

    // ── create ──────────────────────────────────────────────────────────

    @GetMapping("/new")
    public String newRepositoryForm(Model model) {
        model.addAttribute("title", "New repository");
        return "repository/new";
    }

    @PostMapping("/new")
    public String createRepository(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam(defaultValue = "PUBLIC") String visibility,
            @RequestParam(defaultValue = "main") String defaultBranch,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
            RedirectAttributes redirectAttributes) {

        User owner = userService.findByUsername(principal.getUsername())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
        try {
            Repository repo = repositoryService.create(
                    Repository.OwnerType.USER.name(), owner.getId(),
                    name, description, visibility, defaultBranch);
            return "redirect:/" + owner.getUsername() + "/" + repo.getName();
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("name", name);
            redirectAttributes.addFlashAttribute("description", description);
            return "redirect:/new";
        }
    }

    // ── browse ──────────────────────────────────────────────────────────

    @GetMapping("/{username}/{name}")
    public String repositoryOverview(@PathVariable String username, @PathVariable String name,
                                     Model model,
                                     @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        Repository repo = resolve(username, name);
        requireRead(principal, repo);
        addRepoHeader(model, username, repo);

        Path storagePath = Path.of(repo.getStoragePath());
        String ref = repo.getDefaultBranch();
        String resolved = gitService.resolveRef(storagePath, "refs/heads/" + ref);
        if (resolved.isBlank()) {
            // No commits yet \u2014 show empty state.
            model.addAttribute("empty", true);
            return "repository/code";
        }

        List<TreeEntry> entries = gitService.listTree(storagePath, ref, "");
        List<CommitInfo> commits = gitService.log(storagePath, ref, 5);
        model.addAttribute("ref", ref);
        model.addAttribute("entries", entries);
        model.addAttribute("commits", commits);
        return "repository/code";
    }

    @GetMapping("/{username}/{name}/activity")
    public String repositoryActivity(@PathVariable String username, @PathVariable String name,
                                     Model model,
                                     @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
                                     com.furimeo.gitkoo.activity.ActivityService activityService) {
        Repository repo = resolve(username, name);
        requireRead(principal, repo);
        model.addAttribute("title", "Activity \u00b7 " + username + "/" + name);
        model.addAttribute("owner", username);
        model.addAttribute("repo", repo);
        model.addAttribute("activities", activityService.listByRepository(repo.getId()));
        return "repository/activity";
    }

    @GetMapping("/{username}/{name}/blob/{ref}/**")
    public String fileViewer(@PathVariable String username, @PathVariable String name,
                            @PathVariable String ref,
                            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
                            org.springframework.web.servlet.mvc.support.RedirectAttributes ra,
                            jakarta.servlet.http.HttpServletRequest request,
                            Model model) {
        Repository repo = resolve(username, name);
        requireRead(principal, repo);
        addRepoHeader(model, username, repo);

        // Extract the file path after /blob/{ref}/
        String fullPath = request.getRequestURI();
        String prefix = "/" + username + "/" + name + "/blob/" + ref + "/";
        String filePath = fullPath.startsWith(prefix) ? fullPath.substring(prefix.length()) : "";
        if (filePath.isEmpty()) {
            return "redirect:/" + username + "/" + name;
        }

        Path storagePath = Path.of(repo.getStoragePath());
        String content = gitService.catFile(storagePath, ref, filePath);
        model.addAttribute("ref", ref);
        model.addAttribute("filePath", filePath);
        model.addAttribute("content", content);
        return "repository/file";
    }

    // ── settings ────────────────────────────────────────────────────────

    @GetMapping("/{username}/{name}/settings")
    public String repositorySettings(@PathVariable String username, @PathVariable String name,
                                     Model model,
                                     @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        Repository repo = resolve(username, name);
        requireWrite(principal, repo);
        addRepoHeader(model, username, repo);
        return "repository/settings";
    }

    @PostMapping("/{username}/{name}/settings")
    public String updateRepositorySettings(@PathVariable String username, @PathVariable String name,
                                           @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
                                           @RequestParam(required = false) String description,
                                           @RequestParam String visibility,
                                           @RequestParam String defaultBranch,
                                           RedirectAttributes redirectAttributes) {
        Repository repo = resolve(username, name);
        requireWrite(principal, repo);
        // Name is read-only and intentionally not updated (storage path is id-based,
        // but renaming is out of scope for the MVP settings page).
        repo.setDescription(description);
        repo.setVisibility(visibility);
        repo.setDefaultBranch(defaultBranch);
        repo.setUpdatedAt(OffsetDateTime.now());
        repositoryRepository.save(repo);
        redirectAttributes.addFlashAttribute("success", "Settings saved.");
        return "redirect:/" + username + "/" + name + "/settings";
    }

    private Repository resolve(String username, String name) {
        User owner = userService.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        return repositoryService.findByOwnerAndName(Repository.OwnerType.USER.name(), owner.getId(), name)
                .orElseThrow(() -> new IllegalArgumentException("Repository not found: " + username + "/" + name));
    }

    /** Requires at least READ permission for the authenticated user, else 403. */
    private void requireRead(org.springframework.security.core.userdetails.User principal, Repository repo) {
        User actor = actor(principal);
        if (!permissionService.hasPermission(actor, repo, Permission.READ)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You do not have read access to " + repo.getName());
        }
    }

    /** Requires at least WRITE permission for the authenticated user, else 403. */
    private void requireWrite(org.springframework.security.core.userdetails.User principal, Repository repo) {
        User actor = actor(principal);
        if (!permissionService.hasPermission(actor, repo, Permission.WRITE)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You do not have write access to " + repo.getName());
        }
    }

    /** Resolves the authenticated user, or null when anonymous. */
    private User actor(org.springframework.security.core.userdetails.User principal) {
        if (principal == null || "anonymousUser".equals(principal.getUsername())) {
            return null;
        }
        return userService.findByUsername(principal.getUsername()).orElse(null);
    }

    private void addRepoHeader(Model model, String username, Repository repo) {
        model.addAttribute("title", username + "/" + repo.getName());
        model.addAttribute("owner", username);
        model.addAttribute("repo", repo);
    }
}
