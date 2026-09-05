package com.furimeo.gitkoo.notification;

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

/**
 * Notification center: lists a user's notifications and marks them read (DESIGN.md §116).
 *
 * <p>The two write routes redirect back to the list rather than returning a fragment.
 * The Inertia client follows the redirect and swaps the page's props in place, so the
 * result is the same partial update the old HTMX fragment gave, without a second
 * rendering path to keep in step with the first.
 */
@Controller
@RequestMapping
public class NotificationController {

    private static final String VIEW = "notification/list";

    private final NotificationService notificationService;
    private final UserService userService;

    public NotificationController(NotificationService notificationService, UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    @GetMapping("/notifications")
    public String listNotifications(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
            @RequestParam(required = false) Integer page,
            Model model) {
        populate(currentUser(principal), model, page);
        return VIEW;
    }

    @PostMapping("/notifications/{id}/read")
    public String markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        currentUser(principal);
        notificationService.markAsRead(id);
        return "redirect:/notifications";
    }

    @PostMapping("/notifications/read-all")
    public String markAllAsRead(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        notificationService.markAllAsRead(currentUser(principal).getId());
        return "redirect:/notifications";
    }

    private void populate(User user, Model model, Integer page) {
        model.addAttribute("title", "Notifications");
        var pageOfNotifications = com.furimeo.gitkoo.web.Page.of(
                notificationService.listByUser(user.getId()), page);
        model.addAttribute("notifications", pageOfNotifications.items());
        model.addAttribute("page", pageOfNotifications);
        model.addAttribute("unreadCount", notificationService.unreadCount(user.getId()));
    }

    private User currentUser(org.springframework.security.core.userdetails.User principal) {
        return userService.findByUsername(principal.getUsername())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }
}
