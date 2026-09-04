package com.furimeo.gitkoo.repository;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.furimeo.gitkoo.auth.User;
import com.furimeo.gitkoo.auth.UserService;
import com.furimeo.gitkoo.repository.RepositoryPermissionService.Permission;
import com.furimeo.gitkoo.web.NotFoundException;

/**
 * Star, watch, topics and fork.
 *
 * <p>All four are POST, so {@code SecurityConfig} — which only opens {@code GET} on
 * {@code /{username}/{name}/**} — already requires a signed-in user. Read permission
 * is still checked explicitly: a private repository must not be starrable or forkable
 * by someone who cannot see it.
 */
@Controller
public class SocialController {

    private final RepositoryService repositoryService;
    private final RepositoryPermissionService permissionService;
    private final UserService userService;
    private final SocialService social;

    public SocialController(RepositoryService repositoryService,
                            RepositoryPermissionService permissionService,
                            UserService userService, SocialService social) {
        this.repositoryService = repositoryService;
        this.permissionService = permissionService;
        this.userService = userService;
        this.social = social;
    }

    @PostMapping("/{username}/{name}/star")
    public String star(@PathVariable String username, @PathVariable String name,
                       @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        Repository repo = resolveReadable(username, name, principal);
        social.toggleStar(repo.getId(), actor(principal).getId());
        return "redirect:/" + username + "/" + name;
    }

    @PostMapping("/{username}/{name}/watch")
    public String watch(@PathVariable String username, @PathVariable String name,
                        @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        Repository repo = resolveReadable(username, name, principal);
        social.toggleWatch(repo.getId(), actor(principal).getId());
        return "redirect:/" + username + "/" + name;
    }

    @PostMapping("/{username}/{name}/settings/topics")
    public String setTopics(@PathVariable String username, @PathVariable String name,
                            @RequestParam(required = false) String topics,
                            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
                            RedirectAttributes ra) {
        Repository repo = resolveReadable(username, name, principal);
        if (permissionService.permission(actor(principal), repo).ordinal()
                < Permission.WRITE.ordinal()) {
            throw new org.springframework.security.access.AccessDeniedException("Write access required");
        }
        social.setTopics(repo.getId(), topics);
        ra.addFlashAttribute("success", "Topics updated.");
        return "redirect:/" + username + "/" + name + "/settings";
    }

    /**
     * Forks into the signed-in user's account and lands on the new copy.
     *
     * <p>A failed fork — almost always a name collision — returns to the source
     * repository with the reason, rather than to an error page.
     */
    @PostMapping("/{username}/{name}/fork")
    public String fork(@PathVariable String username, @PathVariable String name,
                       @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
                       RedirectAttributes ra) {
        Repository source = resolveReadable(username, name, principal);
        User me = actor(principal);
        try {
            Repository forked = repositoryService.fork(source.getId(),
                    Repository.OwnerType.USER.name(), me.getId());
            return "redirect:/" + me.getUsername() + "/" + forked.getName();
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/" + username + "/" + name;
        }
    }

    private User actor(org.springframework.security.core.userdetails.User principal) {
        return userService.findByUsername(principal.getUsername())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }

    private Repository resolveReadable(String username, String name,
                                       org.springframework.security.core.userdetails.User principal) {
        User owner = userService.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found: " + username));
        Repository repo = repositoryService
                .findByOwnerAndName(Repository.OwnerType.USER.name(), owner.getId(), name)
                .orElseThrow(() -> new NotFoundException("Repository not found: " + username + "/" + name));

        User viewer = principal == null ? null
                : userService.findByUsername(principal.getUsername()).orElse(null);
        if (permissionService.permission(viewer, repo) == Permission.NONE) {
            // Same shape as a missing repository: a private repo should not confirm
            // its own existence to someone who cannot read it.
            throw new NotFoundException("Repository not found: " + username + "/" + name);
        }
        return repo;
    }
}
