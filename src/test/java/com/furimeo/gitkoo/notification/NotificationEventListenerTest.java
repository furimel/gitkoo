package com.furimeo.gitkoo.notification;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.furimeo.gitkoo.auth.UserService;
import com.furimeo.gitkoo.issue.IssueService;
import com.furimeo.gitkoo.repository.Repository;
import com.furimeo.gitkoo.repository.RepositoryService;

/**
 * Integration test for the notification wiring: opening an issue or PR on a repository
 * notifies the repository owner (DESIGN.md §23, §116).
 *
 * <p>This exercises the {@link NotificationEventListener} via the real {@code ActivityService}
 * event publication, without depending on any creation-controller code owned by other agents.
 */
@SpringBootTest
@Transactional
class NotificationEventListenerTest {

    @Autowired
    private UserService userService;

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private IssueService issueService;

    @Autowired
    private NotificationService notificationService;

    @Test
    void openingIssueNotifiesRepoOwner() {
        var owner = userService.createUser("owner-notif", "owner-notif@example.com", "password123");
        var author = userService.createUser("author-notif", "author-notif@example.com", "password123");

        Repository repo = repositoryService.create(
                Repository.OwnerType.USER.name(), owner.getId(), "notif-repo",
                null, "PUBLIC", "main");

        // The owner opens their own issue — no self-notification.
        issueService.create(repo.getId(), "self issue", "body", owner.getId());
        assertThat(notificationService.listByUser(owner.getId())).isEmpty();

        // Another user opens an issue — the owner is notified.
        issueService.create(repo.getId(), "incoming issue", "body", author.getId());

        var ownerNotifications = notificationService.listByUser(owner.getId());
        assertThat(ownerNotifications).hasSize(1);
        assertThat(ownerNotifications.get(0).isRead()).isFalse();
        assertThat(ownerNotifications.get(0).getType()).isEqualTo("ISSUE_OPENED");
        assertThat(notificationService.unreadCount(owner.getId())).isEqualTo(1);
    }

    @Test
    void ownerNotNotifiedAboutOwnActivity() {
        var owner = userService.createUser("owner-self", "owner-self@example.com", "password123");
        Repository repo = repositoryService.create(
                Repository.OwnerType.USER.name(), owner.getId(), "self-repo",
                null, "PUBLIC", "main");

        issueService.create(repo.getId(), "my issue", "body", owner.getId());

        assertThat(notificationService.listByUser(owner.getId())).isEmpty();
    }
}
