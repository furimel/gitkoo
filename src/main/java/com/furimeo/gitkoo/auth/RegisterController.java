package com.furimeo.gitkoo.auth;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Public registration page for new (non-admin) users (DESIGN.md §43).
 *
 * <p>Only available after first-run setup is complete (an admin exists). If no
 * admin exists yet, the setup redirect filter sends the user to /setup instead.
 */
@Controller
@RequestMapping("/register")
public class RegisterController {

    private final UserService userService;

    public RegisterController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String registerForm(Model model) {
        // If setup is not done, redirect to setup instead.
        if (userService.needsSetup()) {
            return "redirect:/setup";
        }
        model.addAttribute("title", "Create account");
        return "auth/register";
    }

    @PostMapping
    public String register(
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            RedirectAttributes redirectAttributes) {

        if (userService.needsSetup()) {
            return "redirect:/setup";
        }
        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Passwords do not match");
            redirectAttributes.addFlashAttribute("username", username);
            redirectAttributes.addFlashAttribute("email", email);
            return "redirect:/register";
        }
        try {
            userService.createUser(username, email, password);
            redirectAttributes.addFlashAttribute("success", "Account created. You can now sign in.");
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("username", username);
            redirectAttributes.addFlashAttribute("email", email);
            return "redirect:/register";
        }
    }
}
