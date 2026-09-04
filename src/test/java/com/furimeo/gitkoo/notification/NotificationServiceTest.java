package com.furimeo.gitkoo.notification;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration test for {@link NotificationService}.
 *
 * <p>The {@code notifications} table has no foreign key on {@code user_id}, so arbitrary ids
 * can be used without creating user rows (DESIGN.md §116).
 */
@SpringBootTest
@Transactional
class NotificationServiceTest {

    private static final Long USER_ID = 4242L;

    @Autowired
    private NotificationService notificationService;

    @Test
    void recordAndListNotifications() {
        assertThat(notificationService.unreadCount(USER_ID)).isZero();

        notificationService.record(USER_ID, "ISSUE_OPENED", "issue #1 opened", "issue", 100L);
        notificationService.record(USER_ID, "PR_OPENED", "PR #2 opened", "pull_request", 100L);

        assertThat(notificationService.listByUser(USER_ID)).hasSize(2);
        assertThat(notificationService.unreadCount(USER_ID)).isEqualTo(2);
    }

    @Test
    void markAsRead() {
        Notification n = notificationService.record(USER_ID, "ISSUE_OPENED", "issue opened", "issue", 1L);
        assertThat(notificationService.unreadCount(USER_ID)).isEqualTo(1);

        notificationService.markAsRead(n.getId());
        assertThat(notificationService.unreadCount(USER_ID)).isZero();
        assertThat(notificationService.listByUser(USER_ID).get(0).isRead()).isTrue();
    }

    @Test
    void markAllAsRead() {
        notificationService.record(USER_ID, "A", "a", null, null);
        notificationService.record(USER_ID, "B", "b", null, null);
        notificationService.record(USER_ID, "C", "c", null, null);
        assertThat(notificationService.unreadCount(USER_ID)).isEqualTo(3);

        notificationService.markAllAsRead(USER_ID);
        assertThat(notificationService.unreadCount(USER_ID)).isZero();
        assertThat(notificationService.listByUser(USER_ID)).allMatch(Notification::isRead);
    }

    @Test
    void listIsNewestFirst() {
        notificationService.record(USER_ID, "OLD", "old", null, null);
        notificationService.record(USER_ID, "NEW", "new", null, null);
        var list = notificationService.listByUser(USER_ID);
        assertThat(list).hasSize(2);
        assertThat(list.get(0).getType()).isEqualTo("NEW");
        assertThat(list.get(1).getType()).isEqualTo("OLD");
    }
}
