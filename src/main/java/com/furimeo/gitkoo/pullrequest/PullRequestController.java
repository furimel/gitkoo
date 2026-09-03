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

    public PullRequestController(PullRequestService prService, RepositoryService repositoryService,
                                UserService userService, MarkdownService markdownService) {
        this.prService = prService;
        this.repositoryService = repositoryService;
        this.userService = userService;
        this.markdownService = markdownService;
    }

    @GetMapping("/pulls")
    public String listPulls(@PathVariable String username, @PathVariable String name, Model model) {
        Repository repo = resolveRepo(username, name);
        List<PullRequest> pulls = prService.listByRepository(repo.getId());
        model.addAttribute("title", "Pull Requests \u00b7 " + username + "/" + name);
        model.addAttribute("owner", username);
        model.addAttribute("repo", repo);
        model.addAttribute("pulls", pulls);
        return "pr/list";
    }

    @GetMapping("/pulls/new")
    public String newPullForm(@PathVariable String username, @PathVariable String name, Model model) {
        Repository repo = resolveRepo(username, name);
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
        User author = userService.findByUsername(principal.getUsername()).orElseThrow();
        PullRequest pr = prService.create(repo.getId(), title, body, author.getId(),
                sourceBranch, targetBranch);
        return "redirect:/" + username + "/" + name + "/pulls/" + pr.getNumber();
    }

    @GetMapping("/pulls/{number}")
    public String viewPull(@PathVariable String username, @PathVariable String name,
                         @PathVariable Integer number, Model model) {
        Repository repo = resolveRepo(username, name);
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
        return "pr/view";
    }

    @PostMapping("/pulls/{number}/review")
    public String submitReview(@PathVariable String username, @PathVariable String name,
                             @PathVariable Integer number, @RequestParam String state,
                             @RequestParam(required = false) String body,
                             @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        Repository repo = resolveRepo(username, name);
        PullRequest pr = prService.findByRepositoryAndNumber(repo.getId(), number);
        User reviewer = userService.findByUsername(principal.getUsername()).orElseThrow();
        prService.addReview(pr.getId(), reviewer.getId(), state, body);
        return "redirect:/" + username + "/" + name + "/pulls/" + number;
    }

    @PostMapping("/pulls/{number}/merge")
    public String mergePull(@PathVariable String username, @PathVariable String name,
                           @PathVariable Integer number) {
        Repository repo = resolveRepo(username, name);
        PullRequest pr = prService.findByRepositoryAndNumber(repo.getId(), number);
        Path storagePath = Path.of(repo.getStoragePath());
        prService.merge(pr.getId(), storagePath);
        return "redirect:/" + username + "/" + name + "/pulls/" + number;
    }

    @PostMapping("/pulls/{number}/close")
    public String closePull(@PathVariable String username, @PathVariable String name,
                           @PathVariable Integer number) {
        Repository repo = resolveRepo(username, name);
        PullRequest pr = prService.findByRepositoryAndNumber(repo.getId(), number);
        prService.close(pr.getId());
        return "redirect:/" + username + "/" + name + "/pulls/" + number;
    }

    private Repository resolveRepo(String username, String name) {
        User owner = userService.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return repositoryService.findByOwnerAndName(Repository.OwnerType.USER.name(), owner.getId(), name)
                .orElseThrow(() -> new IllegalArgumentException("Repository not found"));
    }
}
