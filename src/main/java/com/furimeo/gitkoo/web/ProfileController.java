package com.furimeo.gitkoo.web;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.NoHandlerFoundException;

import com.furimeo.gitkoo.auth.User;
import com.furimeo.gitkoo.auth.UserService;
import com.furimeo.gitkoo.repository.Repository;
import com.furimeo.gitkoo.repository.RepositoryPermissionService;
import com.furimeo.gitkoo.repository.RepositoryPermissionService.Permission;
import com.furimeo.gitkoo.repository.RepositoryService;

/**
 * A user's public profile at {@code /@/{username}}.
 *
 * <p>The header, the repository header, team member lists and the admin user list
 * all linked here already; nothing served the route, so every one of those links
 * was a dead end.
 *
 * <p>The repository list is filtered through {@link RepositoryPermissionService},
 * so a visitor sees someone's private repositories only if they may already read
 * them.
 */
@Controller
public class ProfileController {

    private final UserService userService;
    private final RepositoryService repositoryService;
    private final RepositoryPermissionService permissionService;

    public ProfileController(UserService userService, RepositoryService repositoryService,
                             RepositoryPermissionService permissionService) {
        this.userService = userService;
        this.repositoryService = repositoryService;
        this.permissionService = permissionService;
    }

    @GetMapping("/@/{username}")
    public String profile(@PathVariable String username, Model model,
                          @org.springframework.web.bind.annotation.RequestParam(required = false) Integer page,
                          @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal)
            throws NoHandlerFoundException {
        User profile = userService.findByUsername(username)
                .orElseThrow(() -> new NoHandlerFoundException("GET", "/@/" + username, null));

        User actor = principal == null || "anonymousUser".equals(principal.getUsername())
                ? null
                : userService.findByUsername(principal.getUsername()).orElse(null);

        List<Repository> visible = repositoryService
                .findByOwner(Repository.OwnerType.USER.name(), profile.getId())
                .stream()
                .filter(repo -> permissionService.hasPermission(actor, repo, Permission.READ))
                .toList();

        model.addAttribute("title", profile.getDisplayName() != null
                ? profile.getDisplayName() : profile.getUsername());
        model.addAttribute("profile", profile);
        model.addAttribute("repos", Page.of(visible, page).items());
        model.addAttribute("page", Page.of(visible, page));
        model.addAttribute("repoCount", visible.size());
        model.addAttribute("isSelf", actor != null && actor.getId().equals(profile.getId()));
        return "profile";
    }
}
