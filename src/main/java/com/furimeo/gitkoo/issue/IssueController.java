package com.furimeo.gitkoo.issue;

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
import com.furimeo.gitkoo.repository.Repository;
import com.furimeo.gitkoo.repository.RepositoryPermissionService;
import com.furimeo.gitkoo.repository.RepositoryPermissionService.Permission;
import com.furimeo.gitkoo.repository.RepositoryService;
import com.furimeo.gitkoo.web.MarkdownService;

/**
 * Issue list, creation, and detail pages (DESIGN.md §18).
 */
@Controller
@RequestMapping("/{username}/{name}")
public class IssueController {

    private final IssueService issueService;
    private final RepositoryService repositoryService;
    private final UserService userService;
    private final MarkdownService markdownService;
    private final RepositoryPermissionService permissionService;

    private final LabelService labelService;

    private final com.furimeo.gitkoo.repository.RepoChrome repoChrome;

    public IssueController(IssueService issueService, RepositoryService repositoryService,
                           UserService userService, MarkdownService markdownService,
                           RepositoryPermissionService permissionService,
                           LabelService labelService,
                                com.furimeo.gitkoo.repository.RepoChrome repoChrome) {
        this.issueService = issueService;
        this.repositoryService = repositoryService;
        this.userService = userService;
        this.markdownService = markdownService;
        this.permissionService = permissionService;
        this.labelService = labelService;
            this.repoChrome = repoChrome;
    }

    /**
     * Issue list, optionally narrowed by state.
     *
     * @param state {@code closed} to show closed issues; anything else shows open ones,
     *              matching GitHub's default of hiding what is already dealt with
     */
    @GetMapping("/issues")
    public String listIssues(@PathVariable String username, @PathVariable String name, Model model,
                             @RequestParam(required = false) String state,
                             @RequestParam(required = false) Integer page,
                             @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        Repository repo = resolveRepo(username, name);
        requireRead(principal, repo);

        List<Issue> all = issueService.listByRepository(repo.getId());
        boolean showClosed = "closed".equalsIgnoreCase(state);
        List<Issue> matching = all.stream()
                .filter(i -> showClosed != "OPEN".equals(i.getStatus()))
                .toList();
        var pageOfIssues = com.furimeo.gitkoo.web.Page.of(matching, page);

        repoChrome.apply(model, username, repo,
                principal == null ? null : principal.getUsername());
        model.addAttribute("issues", pageOfIssues.items());
        model.addAttribute("page", pageOfIssues);
        model.addAttribute("filter", showClosed ? "closed" : "open");
        // Counts come from the unfiltered list so both tabs always show a total.
        model.addAttribute("openCount", all.stream().filter(i -> "OPEN".equals(i.getStatus())).count());
        model.addAttribute("closedCount", all.stream().filter(i -> !"OPEN".equals(i.getStatus())).count());
        // Only the visible page is looked up, and in one batch rather than per row.
        model.addAttribute("issueLabels", labelService.labelsByIssue(repo.getId(),
                pageOfIssues.items().stream().map(Issue::getId).toList()));
        return "issue/list";
    }

    @GetMapping("/issues/new")
    public String newIssueForm(@PathVariable String username, @PathVariable String name, Model model,
                               @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        Repository repo = resolveRepo(username, name);
        requireWrite(principal, repo);
        repoChrome.apply(model, username, repo,
                principal == null ? null : principal.getUsername());
        return "issue/new";
    }

    @PostMapping("/issues/new")
    public String createIssue(@PathVariable String username, @PathVariable String name,
                             @RequestParam String title, @RequestParam String body,
                             @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
                             RedirectAttributes ra) {
        Repository repo = resolveRepo(username, name);
        requireWrite(principal, repo);
        User author = userService.findByUsername(principal.getUsername()).orElseThrow();
        Issue issue = issueService.create(repo.getId(), title, body, author.getId());
        return "redirect:/" + username + "/" + name + "/issues/" + issue.getNumber();
    }

    @GetMapping("/issues/{number}")
    public String viewIssue(@PathVariable String username, @PathVariable String name,
                           @PathVariable Integer number, Model model,
                           @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        Repository repo = resolveRepo(username, name);
        requireRead(principal, repo);
        Issue issue = issueService.findByRepositoryAndNumber(repo.getId(), number);
        if (issue == null) {
            model.addAttribute("error", "Issue not found");
            return "redirect:/" + username + "/" + name + "/issues";
        }
        List<IssueComment> comments = issueService.listComments(issue.getId());
        var author = userService.findById(issue.getAuthorId());
        repoChrome.apply(model, username, repo,
                principal == null ? null : principal.getUsername());
        model.addAttribute("issue", issue);
        model.addAttribute("issueBodyHtml", markdownService.render(issue.getBody()));
        model.addAttribute("comments", comments);
        model.addAttribute("commentAuthors", comments.stream()
                .map(c -> userService.findById(c.getAuthorId()).orElse(null))
                .toList());
        // Rendered here rather than in the view. The template used to call the
        // Markdown service itself, which meant handing the whole service to the view
        // layer; the client gets HTML, and only HTML.
        model.addAttribute("commentBodies", comments.stream()
                .map(c -> markdownService.render(c.getBody()))
                .toList());
        model.addAttribute("author", author.orElse(null));

        /*
         * Split the repository's labels here rather than in the view. The template
         * used to do it with a SpEL selection, `repoLabels.?[!attachedIds.contains(id)]`,
         * which cannot work: inside a selection the evaluation context is the element,
         * so `attachedIds` resolved against a Label and threw. It only stayed hidden
         * while every repository had zero labels and the predicate never ran.
         */
        var attachedIds = labelService.getIssueLabels(issue.getId()).stream()
                .map(IssueLabel::getLabelId)
                .collect(java.util.stream.Collectors.toSet());
        var repoLabels = labelService.listLabels(repo.getId());
        model.addAttribute("attachedLabels",
                repoLabels.stream().filter(l -> attachedIds.contains(l.getId())).toList());
        model.addAttribute("availableLabels",
                repoLabels.stream().filter(l -> !attachedIds.contains(l.getId())).toList());
        return "issue/view";
    }

    @PostMapping("/issues/{number}/comment")
    public String addComment(@PathVariable String username, @PathVariable String name,
                            @PathVariable Integer number, @RequestParam String body,
                            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        Repository repo = resolveRepo(username, name);
        requireWrite(principal, repo);
        Issue issue = issueService.findByRepositoryAndNumber(repo.getId(), number);
        User author = userService.findByUsername(principal.getUsername()).orElseThrow();
        issueService.addComment(issue.getId(), body, author.getId());
        return "redirect:/" + username + "/" + name + "/issues/" + number;
    }

    @PostMapping("/issues/{number}/close")
    public String closeIssue(@PathVariable String username, @PathVariable String name,
                            @PathVariable Integer number,
                            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        Repository repo = resolveRepo(username, name);
        requireWrite(principal, repo);
        Issue issue = issueService.findByRepositoryAndNumber(repo.getId(), number);
        if (issue != null) issueService.close(issue.getId());
        return "redirect:/" + username + "/" + name + "/issues/" + number;
    }

    @PostMapping("/issues/{number}/reopen")
    public String reopenIssue(@PathVariable String username, @PathVariable String name,
                             @PathVariable Integer number,
                             @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        Repository repo = resolveRepo(username, name);
        requireWrite(principal, repo);
        Issue issue = issueService.findByRepositoryAndNumber(repo.getId(), number);
        if (issue != null) issueService.reopen(issue.getId());
        return "redirect:/" + username + "/" + name + "/issues/" + number;
    }

    private Repository resolveRepo(String username, String name) {
        User owner = userService.findByUsername(username)
                .orElseThrow(() -> new com.furimeo.gitkoo.web.NotFoundException("User not found: " + username));
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
