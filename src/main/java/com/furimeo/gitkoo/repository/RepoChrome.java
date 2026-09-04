package com.furimeo.gitkoo.repository;

import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import com.furimeo.gitkoo.auth.User;
import com.furimeo.gitkoo.auth.UserService;
import com.furimeo.gitkoo.issue.IssueRepository;
import com.furimeo.gitkoo.pullrequest.PullRequestRepository;

/**
 * The repository title band and tab bar, in one place.
 *
 * <p>Six controllers render a page under {@code /{owner}/{repo}}, and each one used to
 * decide separately what the band above it knew about. The result was tab counters
 * that appeared on some pages and not others. Every one of them calls this instead,
 * so the band is identical wherever you land.
 */
@Component
public class RepoChrome {

    private final UserService userService;
    private final RepositoryRepository repositories;
    private final IssueRepository issues;
    private final PullRequestRepository pulls;
    private final SocialService social;

    public RepoChrome(UserService userService, RepositoryRepository repositories,
                      IssueRepository issues, PullRequestRepository pulls, SocialService social) {
        this.userService = userService;
        this.repositories = repositories;
        this.issues = issues;
        this.pulls = pulls;
        this.social = social;
    }

    /**
     * @param owner the owner's username as it appears in the URL
     * @param viewer the signed-in username, or null for an anonymous visitor
     */
    public void apply(Model model, String owner, Repository repo, String viewer) {
        model.addAttribute("title", owner + "/" + repo.getName());
        model.addAttribute("owner", owner);
        model.addAttribute("repo", repo);

        model.addAttribute("openIssueCount",
                issues.countByRepositoryIdAndStatus(repo.getId(), "OPEN"));
        model.addAttribute("openPrCount",
                pulls.countByRepositoryIdAndStatus(repo.getId(), "OPEN"));

        model.addAttribute("starCount", social.starCount(repo.getId()));
        model.addAttribute("watcherCount", social.watcherCount(repo.getId()));

        Long viewerId = viewer == null ? null
                : userService.findByUsername(viewer).map(User::getId).orElse(null);
        model.addAttribute("starred", social.isStarredBy(repo.getId(), viewerId));
        model.addAttribute("watching", social.isWatchedBy(repo.getId(), viewerId));

        model.addAttribute("forkedFrom", forkLabel(repo));
    }

    /** "owner/name" of the upstream repository, or null when this is not a fork. */
    private String forkLabel(Repository repo) {
        if (repo.getForkOfId() == null) {
            return null;
        }
        return repositories.findById(repo.getForkOfId())
                .map(source -> userService.findById(source.getOwnerId())
                        .map(User::getUsername)
                        .map(name -> name + "/" + source.getName())
                        .orElse(null))
                .orElse(null);
    }
}
