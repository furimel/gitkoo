package com.furimeo.gitkoo.auth;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * First-run setup: creates the initial administrator account (DESIGN.md §68).
 *
 * <p>The page is only useful when no admin exists yet. Once setup is complete, it
 * redirects to the dashboard.
 */
@Controller
@RequestMapping("/setup")
public class SetupController {

    private final UserService userService;

    public SetupController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String setupForm(Model model) {
        if (!userService.needsSetup()) {
            return "redirect:/";
        }
        model.addAttribute("title", "Welcome to GitKoo");
        return "setup";
    }

    @PostMapping
    public String createAdmin(
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            RedirectAttributes redirectAttributes) {

        if (!userService.needsSetup()) {
            return "redirect:/";
        }
        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Passwords do not match");
            redirectAttributes.addFlashAttribute("username", username);
            redirectAttributes.addFlashAttribute("email", email);
            return "redirect:/setup";
        }
        try {
            userService.createAdministrator(username, email, password);
            redirectAttributes.addFlashAttribute("success", "Administrator account created. You can now log in.");
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("username", username);
            redirectAttributes.addFlashAttribute("email", email);
            return "redirect:/setup";
        }
    }
}
