package com.furimeo.gitkoo.notification;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records and lists user notifications (DESIGN.md §116).
 *
 * <p>Notifications are user-oriented (not a social feed): they tell a user that something
 * relevant to them happened, e.g. a new issue or PR on a repository they own.
 */
@Service
public class NotificationService {

    private final NotificationRepository repository;

    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    /**
     * Records a notification for a user.
     *
     * @param userId      the user to notify
     * @param type        a short event type, e.g. {@code ISSUE_OPENED}
     * @param message     human-readable text shown in the notification center
     * @param targetType  optional link target type, e.g. {@code issue} or {@code pull_request}
     * @param targetId    optional id of the linked object
     */
    @Transactional
    public Notification record(Long userId, String type, String message, String targetType, Long targetId) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type);
        n.setMessage(message);
        n.setTargetType(targetType);
        n.setTargetId(targetId);
        n.setRead(false);
        n.setCreatedAt(OffsetDateTime.now());
        return repository.save(n);
    }

    /** Lists a user's notifications, newest first. */
    public List<Notification> listByUser(Long userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /** Marks a single notification as read. */
    @Transactional
    public void markAsRead(Long id) {
        repository.findById(id).ifPresent(n -> {
            n.setRead(true);
            repository.save(n);
        });
    }

    /** Marks every notification for a user as read. */
    @Transactional
    public void markAllAsRead(Long userId) {
        repository.markAllAsRead(userId);
    }

    /** Counts a user's unread notifications (for the header badge). */
    public int unreadCount(Long userId) {
        return repository.countUnread(userId);
    }
}
