package com.furimeo.gitkoo.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.furimeo.gitkoo.auth.SshKey;
import com.furimeo.gitkoo.auth.SshKeyService;
import com.furimeo.gitkoo.auth.User;
import com.furimeo.gitkoo.auth.UserService;

/**
 * User SSH key management UI (DESIGN.md §8, §43).
 *
 * <p>Authenticated users can register and remove their own public keys, which the SSH
 * server matches by fingerprint during Git access (see {@code GitSshServer}).
 */
@Controller
@RequestMapping("/settings/keys")
public class SshKeyController {

    private final SshKeyService sshKeyService;
    private final UserService userService;

    public SshKeyController(SshKeyService sshKeyService, UserService userService) {
        this.sshKeyService = sshKeyService;
        this.userService = userService;
    }

    @GetMapping
    public String listKeys(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
                          Model model) {
        User user = resolveUser(principal);
        model.addAttribute("title", "SSH keys");
        model.addAttribute("keys", sshKeyService.listByUser(user.getId()));
        return "settings/keys";
    }

    @PostMapping
    public String addKey(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
                        @RequestParam String title,
                        @RequestParam String publicKey,
                        RedirectAttributes redirectAttributes) {
        User user = resolveUser(principal);
        try {
            sshKeyService.addKey(user.getId(), title, publicKey);
            redirectAttributes.addFlashAttribute("success", "SSH key added.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("title", title);
            redirectAttributes.addFlashAttribute("publicKey", publicKey);
        }
        return "redirect:/settings/keys";
    }

    @PostMapping("/{id}/delete")
    public String deleteKey(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
                           @PathVariable Long id,
                           RedirectAttributes redirectAttributes) {
        User user = resolveUser(principal);
        SshKey key = sshKeyService.findById(id).orElse(null);
        if (key == null || !key.getUserId().equals(user.getId())) {
            redirectAttributes.addFlashAttribute("error", "SSH key not found.");
            return "redirect:/settings/keys";
        }
        sshKeyService.delete(id);
        redirectAttributes.addFlashAttribute("success", "SSH key removed.");
        return "redirect:/settings/keys";
    }

    private User resolveUser(org.springframework.security.core.userdetails.User principal) {
        return userService.findByUsername(principal.getUsername())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }
}
