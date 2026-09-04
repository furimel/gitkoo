package com.furimeo.gitkoo.pullrequest;

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

import com.furimeo.gitkoo.auth.User;
import com.furimeo.gitkoo.auth.UserService;
import com.furimeo.gitkoo.repository.Repository;
import com.furimeo.gitkoo.repository.RepositoryPermissionService;
import com.furimeo.gitkoo.repository.RepositoryPermissionService.Permission;
import com.furimeo.gitkoo.repository.RepositoryService;
import com.furimeo.gitkoo.web.MarkdownService;

/** Pull request list, creation, view, review, and merge (DESIGN.md §15, §16, §17). */
@Controller
@RequestMapping("/{username}/{name}")
public class PullRequestController {

    private final PullRequestService prService;
    private final RepositoryService repositoryService;
    private final UserService userService;
    private final MarkdownService markdownService;
    private final RepositoryPermissionService permissionService;
    private final com.furimeo.gitkoo.git.GitService gitService;
    private final com.furimeo.gitkoo.git.DiffParser diffParser;

    public PullRequestController(PullRequestService prService, RepositoryService repositoryService,
                                UserService userService, MarkdownService markdownService,
                                RepositoryPermissionService permissionService,
                                com.furimeo.gitkoo.git.GitService gitService,
                                com.furimeo.gitkoo.git.DiffParser diffParser) {
        this.prService = prService;
        this.repositoryService = repositoryService;
        this.userService = userService;
        this.markdownService = markdownService;
        this.permissionService = permissionService;
        this.gitService = gitService;
        this.diffParser = diffParser;
    }

    @GetMapping("/pulls")
    public String listPulls(@PathVariable String username, @PathVariable String name, Model model,
                            @RequestParam(required = false) String state,
                            @RequestParam(required = false) Integer page,
                            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        Repository repo = resolveRepo(username, name);
        requireRead(principal, repo);

        List<PullRequest> all = prService.listByRepository(repo.getId());
        boolean showClosed = "closed".equalsIgnoreCase(state);
        List<PullRequest> matching = all.stream()
                .filter(p -> showClosed != "OPEN".equals(p.getStatus()))
                .toList();
        var pageOfPulls = com.furimeo.gitkoo.web.Page.of(matching, page);

        model.addAttribute("title", "Pull Requests \u00b7 " + username + "/" + name);
        model.addAttribute("owner", username);
        model.addAttribute("repo", repo);
        model.addAttribute("pulls", pageOfPulls.items());
        model.addAttribute("page", pageOfPulls);
        model.addAttribute("filter", showClosed ? "closed" : "open");
        model.addAttribute("openCount", all.stream().filter(p -> "OPEN".equals(p.getStatus())).count());
        model.addAttribute("closedCount", all.stream().filter(p -> !"OPEN".equals(p.getStatus())).count());
        return "pr/list";
    }

    @GetMapping("/pulls/new")
    public String newPullForm(@PathVariable String username, @PathVariable String name, Model model,
                              @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        Repository repo = resolveRepo(username, name);
        requireWrite(principal, repo);
        model.addAttribute("title", "New pull request \u00b7 " + username + "/" + name);
        model.addAttribute("owner", username);
        model.addAttribute("repo", repo);
        return "pr/new";
    }

    @PostMapping("/pulls/new")
    public String createPull(@PathVariable String username, @PathVariable String name,
                            @RequestParam String title, @RequestParam String body,
                            @RequestParam String sourceBranch, @RequestParam(defaultValue = "main") String targetBranch,
                            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        Repository repo = resolveRepo(username, name);
        requireWrite(principal, repo);
        User author = userService.findByUsername(principal.getUsername()).orElseThrow();
        PullRequest pr = prService.create(repo.getId(), title, body, author.getId(),
                sourceBranch, targetBranch);
        return "redirect:/" + username + "/" + name + "/pulls/" + pr.getNumber();
    }

    @GetMapping("/pulls/{number}")
    public String viewPull(@PathVariable String username, @PathVariable String name,
                         @PathVariable Integer number, Model model,
                         @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        Repository repo = resolveRepo(username, name);
        requireRead(principal, repo);
        PullRequest pr = prService.findByRepositoryAndNumber(repo.getId(), number);
        if (pr == null) {
            return "redirect:/" + username + "/" + name + "/pulls";
        }
        var author = userService.findById(pr.getAuthorId());
        List<PullRequestReview> reviews = prService.listReviews(pr.getId());
        model.addAttribute("title", "PR #" + number + " \u00b7 " + username + "/" + name);
        model.addAttribute("owner", username);
        model.addAttribute("repo", repo);
        model.addAttribute("pr", pr);
        model.addAttribute("prBodyHtml", markdownService.render(pr.getBody()));
        model.addAttribute("author", author.orElse(null));
        model.addAttribute("reviews", reviews);
        model.addAttribute("reviewers", reviews.stream()
                .map(r -> userService.findById(r.getReviewerId()).orElse(null))
                .toList());
        model.addAttribute("markdown", markdownService);

        // The changes the PR proposes, so the page can actually be reviewed.
        var storagePath = java.nio.file.Path.of(repo.getStoragePath());
        var files = diffParser.parse(
                gitService.diff(storagePath, pr.getTargetBranch(), pr.getSourceBranch()));
        int[] totals = diffParser.totals(files);
        model.addAttribute("diffFiles", files);
        model.addAttribute("additions", totals[0]);
        model.addAttribute("deletions", totals[1]);
        model.addAttribute("commits",
                gitService.log(storagePath, pr.getTargetBranch() + ".." + pr.getSourceBranch(), 50));
        model.addAttribute("mergesCleanly",
                gitService.mergesCleanly(storagePath, pr.getTargetBranch(), pr.getSourceBranch()));
        return "pr/view";
    }

    @PostMapping("/pulls/{number}/review")
    public String submitReview(@PathVariable String username, @PathVariable String name,
                             @PathVariable Integer number, @RequestParam String state,
                             @RequestParam(required = false) String body,
                             @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        Repository repo = resolveRepo(username, name);
        requireWrite(principal, repo);
        PullRequest pr = prService.findByRepositoryAndNumber(repo.getId(), number);
        User reviewer = userService.findByUsername(principal.getUsername()).orElseThrow();
        prService.addReview(pr.getId(), reviewer.getId(), state, body);
        return "redirect:/" + username + "/" + name + "/pulls/" + number;
    }

    @PostMapping("/pulls/{number}/merge")
    public String mergePull(@PathVariable String username, @PathVariable String name,
                           @PathVariable Integer number,
                           @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        Repository repo = resolveRepo(username, name);
        requireWrite(principal, repo);
        PullRequest pr = prService.findByRepositoryAndNumber(repo.getId(), number);
        Path storagePath = Path.of(repo.getStoragePath());
        prService.merge(pr.getId(), storagePath);
        return "redirect:/" + username + "/" + name + "/pulls/" + number;
    }

    @PostMapping("/pulls/{number}/close")
    public String closePull(@PathVariable String username, @PathVariable String name,
                           @PathVariable Integer number,
                           @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        Repository repo = resolveRepo(username, name);
        requireWrite(principal, repo);
        PullRequest pr = prService.findByRepositoryAndNumber(repo.getId(), number);
        prService.close(pr.getId());
        return "redirect:/" + username + "/" + name + "/pulls/" + number;
    }

    private Repository resolveRepo(String username, String name) {
        User owner = userService.findByUsername(username)
                .orElseThrow(() -> new com.furimeo.gitkoo.web.NotFoundException("User not found"));
        return repositoryService.findByOwnerAndName(Repository.OwnerType.USER.name(), owner.getId(), name)
                .orElseThrow(() -> new com.furimeo.gitkoo.web.NotFoundException("Repository not found"));
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
}
