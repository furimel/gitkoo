package com.furimeo.gitkoo.web.inertia;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.furimeo.gitkoo.auth.User;
import com.furimeo.gitkoo.auth.UserService;
import com.furimeo.gitkoo.notification.NotificationService;

/**
 * Props every page gets, whatever its controller put in the model.
 *
 * <p>Inertia calls these shared data. They are the things the app chrome needs on
 * every screen - who is signed in, whether they are an administrator, how many
 * notifications are waiting - and threading them through thirty controllers by hand
 * is how they come to be present on some pages and missing on others.
 */
@Component
public class SharedProps {

    /**
     * An anonymous visitor, as the client sees them.
     *
     * <p>{@code Map.of} rejects a null value, so this cannot be written inline: doing
     * so throws a NullPointerException on every page an anonymous visitor loads.
     */
    private static final Map<String, Object> ANONYMOUS =
            Collections.unmodifiableMap(anonymousMap());

    private static Map<String, Object> anonymousMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("user", null);
        return map;
    }

    private final UserService userService;
    private final NotificationService notificationService;

    public SharedProps(UserService userService, NotificationService notificationService) {
        this.userService = userService;
        this.notificationService = notificationService;
    }

    Map<String, Object> forEveryPage() {
        Map<String, Object> shared = new LinkedHashMap<>();
        shared.put("auth", auth());
        return shared;
    }

    private Map<String, Object> auth() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return ANONYMOUS;
        }

        User user = userService.findByUsername(authentication.getName()).orElse(null);
        if (user == null) {
            return ANONYMOUS;
        }

        Map<String, Object> account = new LinkedHashMap<>();
        account.put("username", user.getUsername());
        account.put("displayName", user.getDisplayName());
        account.put("admin", user.isAdmin());
        account.put("unread", notificationService.unreadCount(user.getId()));

        return Map.of("user", account);
    }
}
