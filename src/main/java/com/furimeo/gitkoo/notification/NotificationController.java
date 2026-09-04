package com.furimeo.gitkoo.notification;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

import com.furimeo.gitkoo.auth.User;
import com.furimeo.gitkoo.auth.UserService;

/**
 * Notification center: lists a user's notifications and marks them read (DESIGN.md §116).
 *
 * <p>The list page polls every 30 seconds via HTMX so the unread badge stays fresh without a
 * full page reload. HTMX requests swap just the {@code panel} fragment; full loads render the
 * whole page.
 */
@Controller
@RequestMapping
public class NotificationController {

    private static final String VIEW = "notification/list";
    private static final String PANEL_FRAGMENT = "notification/list :: panel";

    private final NotificationService notificationService;
    private final UserService userService;

    public NotificationController(NotificationService notificationService, UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    @GetMapping("/notifications")
    public String listNotifications(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
                                    @RequestHeader(value = "HX-Request", required = false) String hxRequest,
                                    @org.springframework.web.bind.annotation.RequestParam(required = false) Integer page,
                                    Model model) {
        User user = currentUser(principal);
        populate(user, model, page);
        return isHtmx(hxRequest) ? PANEL_FRAGMENT : VIEW;
    }

    /** Marks a single notification as read. Returns the panel fragment for HTMX swaps. */
    @PostMapping("/notifications/{id}/read")
    public String markAsRead(@PathVariable Long id,
                            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
                            @RequestHeader(value = "HX-Request", required = false) String hxRequest,
                            Model model) {
        User user = currentUser(principal);
        notificationService.markAsRead(id);
        populate(user, model);
        return isHtmx(hxRequest) ? PANEL_FRAGMENT : VIEW;
    }

    /** Marks every notification for the current user as read. */
    @PostMapping("/notifications/read-all")
    public String markAllAsRead(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
                               @RequestHeader(value = "HX-Request", required = false) String hxRequest,
                               Model model) {
        User user = currentUser(principal);
        notificationService.markAllAsRead(user.getId());
        populate(user, model);
        return isHtmx(hxRequest) ? PANEL_FRAGMENT : VIEW;
    }

    private void populate(User user, Model model) {
        populate(user, model, null);
    }

    private void populate(User user, Model model, Integer page) {
        // title is needed whenever the full page renders (no-JS POST fallback); the HTMX
        // fragment path ignores it.
        model.addAttribute("title", "Notifications");
        var pageOfNotifications = com.furimeo.gitkoo.web.Page.of(
                notificationService.listByUser(user.getId()), page);
        model.addAttribute("notifications", pageOfNotifications.items());
        model.addAttribute("page", pageOfNotifications);
        model.addAttribute("unreadCount", notificationService.unreadCount(user.getId()));
    }

    private static boolean isHtmx(String hxRequest) {
        return "true".equalsIgnoreCase(hxRequest);
    }

    private User currentUser(org.springframework.security.core.userdetails.User principal) {
        return userService.findByUsername(principal.getUsername())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }
}
