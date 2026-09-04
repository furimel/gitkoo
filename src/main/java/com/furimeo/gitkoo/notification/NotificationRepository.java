package com.furimeo.gitkoo.notification;

import java.util.List;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data JDBC repository for {@link Notification}.
 *
 * @see DESIGN.md §87 (no generic BaseRepository, concrete per domain)
 */
public interface NotificationRepository extends CrudRepository<Notification, Long> {

    /** Lists a user's notifications, newest first. */
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** Lists a user's notifications filtered by read state (0 = unread, 1 = read). */
    List<Notification> findByUserIdAndRead(Long userId, Integer read);

    /** Counts a user's unread notifications. */
    @Query("SELECT COUNT(*) FROM notifications WHERE user_id = :userId AND read = 0")
    int countUnread(@Param("userId") Long userId);

    /** Marks a single notification as read. */
    @Modifying
    @Query("UPDATE notifications SET read = 1 WHERE id = :id")
    void markAsRead(@Param("id") Long id);

    /** Marks every notification for a user as read. */
    @Modifying
    @Query("UPDATE notifications SET read = 1 WHERE user_id = :userId AND read = 0")
    void markAllAsRead(@Param("userId") Long userId);
}
