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

    public IssueController(IssueService issueService, RepositoryService repositoryService,
                           UserService userService, MarkdownService markdownService) {
        this.issueService = issueService;
        this.repositoryService = repositoryService;
        this.userService = userService;
        this.markdownService = markdownService;
    }

    @GetMapping("/issues")
    public String listIssues(@PathVariable String username, @PathVariable String name, Model model) {
        Repository repo = resolveRepo(username, name);
        List<Issue> issues = issueService.listByRepository(repo.getId());
        model.addAttribute("title", "Issues \u00b7 " + username + "/" + name);
        model.addAttribute("owner", username);
        model.addAttribute("repo", repo);
        model.addAttribute("issues", issues);
        return "issue/list";
    }

    @GetMapping("/issues/new")
    public String newIssueForm(@PathVariable String username, @PathVariable String name, Model model) {
        Repository repo = resolveRepo(username, name);
        model.addAttribute("title", "New issue \u00b7 " + username + "/" + name);
        model.addAttribute("owner", username);
        model.addAttribute("repo", repo);
        return "issue/new";
    }

    @PostMapping("/issues/new")
    public String createIssue(@PathVariable String username, @PathVariable String name,
                             @RequestParam String title, @RequestParam String body,
                             @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
                             RedirectAttributes ra) {
        Repository repo = resolveRepo(username, name);
        User author = userService.findByUsername(principal.getUsername()).orElseThrow();
        Issue issue = issueService.create(repo.getId(), title, body, author.getId());
        return "redirect:/" + username + "/" + name + "/issues/" + issue.getNumber();
    }

    @GetMapping("/issues/{number}")
    public String viewIssue(@PathVariable String username, @PathVariable String name,
                           @PathVariable Integer number, Model model) {
        Repository repo = resolveRepo(username, name);
        Issue issue = issueService.findByRepositoryAndNumber(repo.getId(), number);
        if (issue == null) {
            model.addAttribute("error", "Issue not found");
            return "redirect:/" + username + "/" + name + "/issues";
        }
        List<IssueComment> comments = issueService.listComments(issue.getId());
        var author = userService.findById(issue.getAuthorId());
        model.addAttribute("title", "#" + number + " \u00b7 " + username + "/" + name);
        model.addAttribute("owner", username);
        model.addAttribute("repo", repo);
        model.addAttribute("issue", issue);
        model.addAttribute("issueBodyHtml", markdownService.render(issue.getBody()));
        model.addAttribute("comments", comments);
        model.addAttribute("commentAuthors", comments.stream()
                .map(c -> userService.findById(c.getAuthorId()).orElse(null))
                .toList());
        model.addAttribute("author", author.orElse(null));
        model.addAttribute("markdown", markdownService);
        return "issue/view";
    }

    @PostMapping("/issues/{number}/comment")
    public String addComment(@PathVariable String username, @PathVariable String name,
                            @PathVariable Integer number, @RequestParam String body,
                            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        Repository repo = resolveRepo(username, name);
        Issue issue = issueService.findByRepositoryAndNumber(repo.getId(), number);
        User author = userService.findByUsername(principal.getUsername()).orElseThrow();
        issueService.addComment(issue.getId(), body, author.getId());
        return "redirect:/" + username + "/" + name + "/issues/" + number;
    }

    @PostMapping("/issues/{number}/close")
    public String closeIssue(@PathVariable String username, @PathVariable String name,
                            @PathVariable Integer number) {
        Repository repo = resolveRepo(username, name);
        Issue issue = issueService.findByRepositoryAndNumber(repo.getId(), number);
        if (issue != null) issueService.close(issue.getId());
        return "redirect:/" + username + "/" + name + "/issues/" + number;
    }

    @PostMapping("/issues/{number}/reopen")
    public String reopenIssue(@PathVariable String username, @PathVariable String name,
                             @PathVariable Integer number) {
        Repository repo = resolveRepo(username, name);
        Issue issue = issueService.findByRepositoryAndNumber(repo.getId(), number);
        if (issue != null) issueService.reopen(issue.getId());
        return "redirect:/" + username + "/" + name + "/issues/" + number;
    }

    private Repository resolveRepo(String username, String name) {
        User owner = userService.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        return repositoryService.findByOwnerAndName(Repository.OwnerType.USER.name(), owner.getId(), name)
                .orElseThrow(() -> new IllegalArgumentException("Repository not found"));
    }
}
