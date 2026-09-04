package com.furimeo.gitkoo.repository;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.furimeo.gitkoo.auth.User;
import com.furimeo.gitkoo.auth.UserService;

/**
 * Protects and unprotects repository branches (DESIGN.md §78).
 *
 * <p>Lives under {@code /{username}/{name}/settings/branches} so it does not collide with the
 * repository browsing routes owned by {@link RepositoryController}.
 */
@Controller
@RequestMapping("/{username}/{name}/settings/branches")
public class ProtectedBranchController {

    private final ProtectedBranchService protectedBranchService;
    private final RepositoryService repositoryService;
    private final UserService userService;

    public ProtectedBranchController(ProtectedBranchService protectedBranchService,
                                     RepositoryService repositoryService, UserService userService) {
        this.protectedBranchService = protectedBranchService;
        this.repositoryService = repositoryService;
        this.userService = userService;
    }

    @PostMapping("/protect")
    public String protect(@PathVariable String username, @PathVariable String name,
                         @RequestParam String branch,
                         @RequestParam(defaultValue = "true") boolean requirePr,
                         RedirectAttributes redirectAttributes) {
        Repository repo = resolveRepo(username, name);
        protectedBranchService.protect(repo.getId(), branch, requirePr);
        redirectAttributes.addFlashAttribute("message", "Branch '" + branch + "' is now protected");
        return "redirect:/" + username + "/" + name;
    }

    @PostMapping("/unprotect")
    public String unprotect(@PathVariable String username, @PathVariable String name,
                            @RequestParam String branch, RedirectAttributes redirectAttributes) {
        Repository repo = resolveRepo(username, name);
        protectedBranchService.unprotect(repo.getId(), branch);
        redirectAttributes.addFlashAttribute("message", "Branch '" + branch + "' is no longer protected");
        return "redirect:/" + username + "/" + name;
    }

    private Repository resolveRepo(String username, String name) {
        User owner = userService.findByUsername(username)
                .orElseThrow(() -> new com.furimeo.gitkoo.web.NotFoundException("User not found: " + username));
        return repositoryService.findByOwnerAndName(Repository.OwnerType.USER.name(), owner.getId(), name)
                .orElseThrow(() -> new com.furimeo.gitkoo.web.NotFoundException("Repository not found: " + username + "/" + name));
    }
}
