package com.furimeo.gitkoo.auth;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.furimeo.gitkoo.repository.Repository;
import com.furimeo.gitkoo.repository.RepositoryService;

/**
 * Shows the login page and the dashboard. Spring Security handles actual
 * authentication at {@code POST /login} (DESIGN.md §43).
 */
@Controller
public class LoginController {

    private final UserService userService;
    private final RepositoryService repositoryService;

    public LoginController(UserService userService, RepositoryService repositoryService) {
        this.userService = userService;
        this.repositoryService = repositoryService;
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/")
    public String dashboard(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
                            Model model) {
        model.addAttribute("title", "Dashboard");
        if (principal != null) {
            User user = userService.findByUsername(principal.getUsername()).orElse(null);
            if (user != null) {
                var repos = repositoryService.findByOwner(
                        Repository.OwnerType.USER.name(), user.getId());
                model.addAttribute("repos", repos);
            }
        }
        return "dashboard";
    }
}
