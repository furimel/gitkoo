package com.furimeo.gitkoo.web;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.furimeo.gitkoo.auth.User;
import com.furimeo.gitkoo.auth.UserService;
import com.furimeo.gitkoo.repository.Repository;
import com.furimeo.gitkoo.repository.RepositoryRepository;
import com.furimeo.gitkoo.repository.RepositoryPermissionService;
import com.furimeo.gitkoo.repository.RepositoryPermissionService.Permission;

/**
 * Repository search (DESIGN.md §48).
 *
 * <p>MVP uses a simple name match, no Elasticsearch or vector search, which is
 * enough at the expected instance scale.
 *
 * <p>Results are filtered through {@link RepositoryPermissionService}. The previous
 * implementation ran a bare {@code LIKE} over the whole table, so any signed-in
 * user could enumerate the names and descriptions of every private repository on
 * the instance just by guessing substrings.
 */
@Controller
public class SearchController {

    private final RepositoryRepository repositoryRepository;
    private final UserService userService;
    private final RepositoryPermissionService permissionService;

    public SearchController(RepositoryRepository repositoryRepository, UserService userService,
                            RepositoryPermissionService permissionService) {
        this.repositoryRepository = repositoryRepository;
        this.userService = userService;
        this.permissionService = permissionService;
    }

    /** Cap on results returned, before permission filtering. */
    private static final int LIMIT = 50;

    @GetMapping("/search")
    public String search(@RequestParam(required = false) String q, Model model,
                         @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        model.addAttribute("title", "Search");
        model.addAttribute("query", q);

        if (q == null || q.isBlank()) {
            model.addAttribute("results", List.of());
            return "search";
        }

        User actor = principal == null || "anonymousUser".equals(principal.getUsername())
                ? null
                : userService.findByUsername(principal.getUsername()).orElse(null);
        String needle = q.toLowerCase(java.util.Locale.ROOT);

        List<SearchResult> results = new ArrayList<>();
        for (Repository repo : repositoryRepository.findAll()) {
            if (results.size() >= LIMIT) {
                break;
            }
            if (!matches(repo, needle)) {
                continue;
            }
            // The permission service owns the visibility rules; do not restate them here.
            if (!permissionService.hasPermission(actor, repo, Permission.READ)) {
                continue;
            }
            results.add(new SearchResult(repo.getName(), repo.getDescription(),
                    ownerName(repo), repo.getVisibility()));
        }

        model.addAttribute("results", results);
        return "search";
    }

    private boolean matches(Repository repo, String needle) {
        if (repo.getName() != null && repo.getName().toLowerCase(java.util.Locale.ROOT).contains(needle)) {
            return true;
        }
        return repo.getDescription() != null
                && repo.getDescription().toLowerCase(java.util.Locale.ROOT).contains(needle);
    }

    /** Owner username, so a result can link to the repository. */
    private String ownerName(Repository repo) {
        if (!Repository.OwnerType.USER.name().equals(repo.getOwnerType())) {
            return null;
        }
        return userService.findById(repo.getOwnerId()).map(User::getUsername).orElse(null);
    }

    /** A single search result entry. */
    public record SearchResult(String name, String description, String owner, String visibility) {
    }
}
