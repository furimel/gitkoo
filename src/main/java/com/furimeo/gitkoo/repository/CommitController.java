package com.furimeo.gitkoo.repository;

import java.nio.file.Path;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.furimeo.gitkoo.auth.User;
import com.furimeo.gitkoo.auth.UserService;
import com.furimeo.gitkoo.git.GitService;
import com.furimeo.gitkoo.git.GitService.CommitDetail;

/**
 * Commit detail page and diff fragments (DESIGN.md §13, §15).
 *
 * <p>Renders a single commit (SHA, author, date, message, parent, diff) and serves a
 * reusable diff fragment for pull requests. Repository/PR controllers are owned by other
 * agents, so the PR diff is loaded here via HTMX from {@code pr/view.html} rather than
 * passed as a model attribute by {@code PullRequestController}.
 */
@Controller
@RequestMapping
public class CommitController {

    private final RepositoryService repositoryService;
    private final UserService userService;
    private final GitService gitService;
    private final com.furimeo.gitkoo.git.DiffParser diffParser;

    private final RepositoryPermissionService permissionService;

    private final com.furimeo.gitkoo.repository.RepoChrome repoChrome;

    public CommitController(RepositoryService repositoryService, UserService userService,
                            GitService gitService, com.furimeo.gitkoo.git.DiffParser diffParser,
                            RepositoryPermissionService permissionService,
                                com.furimeo.gitkoo.repository.RepoChrome repoChrome) {
        this.repositoryService = repositoryService;
        this.userService = userService;
        this.gitService = gitService;
        this.diffParser = diffParser;
        this.permissionService = permissionService;
            this.repoChrome = repoChrome;
    }

    /**
     * Requires at least READ for the caller, else 403.
     *
     * <p>This controller previously checked nothing, so every commit and diff in a
     * private repository was readable by any signed-in user. Opening public
     * repositories to anonymous readers would have widened that to the whole
     * internet, so the check has to exist before the security config is relaxed.
     */
    private void requireRead(org.springframework.security.core.userdetails.User principal, Repository repo) {
        User actor = principal == null || "anonymousUser".equals(principal.getUsername())
                ? null
                : userService.findByUsername(principal.getUsername()).orElse(null);
        if (!permissionService.hasPermission(actor, repo, RepositoryPermissionService.Permission.READ)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You do not have read access to " + repo.getName());
        }
    }

    /** Renders the commit detail page (DESIGN.md §13). */
    @GetMapping("/{username}/{name}/commit/{sha}")
    public String commitDetail(@PathVariable String username, @PathVariable String name,
                               @PathVariable String sha, Model model,
                               @org.springframework.security.core.annotation.AuthenticationPrincipal
                               org.springframework.security.core.userdetails.User principal) {
        Repository repo = resolve(username, name);
        requireRead(principal, repo);
        Path storagePath = Path.of(repo.getStoragePath());

        CommitDetail commit = gitService.showCommit(storagePath, sha);
        if (commit == null) {
            return "redirect:/" + username + "/" + name;
        }

        var files = diffParser.parse(gitService.diff(storagePath, sha));
        int[] totals = diffParser.totals(files);

        addRepoHeader(model, username, repo, principal);
        model.addAttribute("commit", commit);
        model.addAttribute("sha", sha);
        model.addAttribute("diffFiles", files);
        model.addAttribute("additions", totals[0]);
        model.addAttribute("deletions", totals[1]);
        return "repository/commit";
    }

    /**
     * Compares two refs (DESIGN.md §15), at /{owner}/{repo}/diff?base=&head=.
     *
     * <p>Branch names may contain slashes, so the refs are query parameters rather
     * than path segments.
     */
    @GetMapping("/{username}/{name}/diff")
    public String compare(@PathVariable String username, @PathVariable String name,
                               @RequestParam String base, @RequestParam String head,
                               Model model,
                               @org.springframework.security.core.annotation.AuthenticationPrincipal
                               org.springframework.security.core.userdetails.User principal) {
        Repository repo = resolve(username, name);
        requireRead(principal, repo);
        // Now a page in its own right. It was an HTMX fragment, and HTMX went with the
        // templates - a route that renders nothing is worse than no route.
        addRepoHeader(model, username, repo, principal);
        Path storagePath = Path.of(repo.getStoragePath());
        var files = diffParser.parse(gitService.diff(storagePath, base, head));
        int[] totals = diffParser.totals(files);
        model.addAttribute("base", base);
        model.addAttribute("head", head);
        model.addAttribute("diffFiles", files);
        model.addAttribute("additions", totals[0]);
        model.addAttribute("deletions", totals[1]);
        return "repository/diff";
    }

    private Repository resolve(String username, String name) {
        User owner = userService.findByUsername(username)
                .orElseThrow(() -> new com.furimeo.gitkoo.web.NotFoundException("User not found: " + username));
        return repositoryService.findByOwnerAndName(Repository.OwnerType.USER.name(), owner.getId(), name)
                .orElseThrow(() -> new com.furimeo.gitkoo.web.NotFoundException("Repository not found: " + username + "/" + name));
    }

    private void addRepoHeader(Model model, String username, Repository repo,
                               org.springframework.security.core.userdetails.User principal) {
        repoChrome.apply(model, username, repo,
                principal == null ? null : principal.getUsername());
    }
}
