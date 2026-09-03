package com.furimeo.gitkoo.repository;

import java.nio.file.Path;
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
    private final UserService userService;
    private final GitService gitService;

    public RepositoryController(RepositoryService repositoryService, UserService userService, GitService gitService) {
        this.repositoryService = repositoryService;
        this.userService = userService;
        this.gitService = gitService;
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
                                     Model model) {
        Repository repo = resolve(username, name);
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

    @GetMapping("/{username}/{name}/blob/{ref}/**")
    public String fileViewer(@PathVariable String username, @PathVariable String name,
                            @PathVariable String ref,
                            org.springframework.web.servlet.mvc.support.RedirectAttributes ra,
                            jakarta.servlet.http.HttpServletRequest request,
                            Model model) {
        Repository repo = resolve(username, name);
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

    private Repository resolve(String username, String name) {
        User owner = userService.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        return repositoryService.findByOwnerAndName(Repository.OwnerType.USER.name(), owner.getId(), name)
                .orElseThrow(() -> new IllegalArgumentException("Repository not found: " + username + "/" + name));
    }

    private void addRepoHeader(Model model, String username, Repository repo) {
        model.addAttribute("title", username + "/" + repo.getName());
        model.addAttribute("owner", username);
        model.addAttribute("repo", repo);
    }
}
