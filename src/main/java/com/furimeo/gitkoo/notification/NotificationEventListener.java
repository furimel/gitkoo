package com.furimeo.gitkoo.notification;

import java.util.Optional;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.furimeo.gitkoo.activity.Activity;
import com.furimeo.gitkoo.activity.ActivityCreatedEvent;
import com.furimeo.gitkoo.repository.Repository;
import com.furimeo.gitkoo.repository.RepositoryService;

/**
 * Records notifications when activities that a user would care about occur (DESIGN.md §23,
 * §116).
 *
 * <p>MVP scope: notify the repository owner when an issue or pull request is opened on their
 * repository. The owner is not notified about their own activity. This listens to
 * {@link ActivityCreatedEvent} so it does not need to be wired into every creation call site
 * (those services are owned by other agents).
 */
@Component
public class NotificationEventListener {

    /** Activity types that should notify the repository owner. */
    private static final String ISSUE_OPENED = "ISSUE_OPENED";
    private static final String PR_OPENED = "PR_OPENED";

    private final NotificationService notificationService;
    private final RepositoryService repositoryService;

    public NotificationEventListener(NotificationService notificationService,
                                      RepositoryService repositoryService) {
        this.notificationService = notificationService;
        this.repositoryService = repositoryService;
    }

    @EventListener
    public void onActivityCreated(ActivityCreatedEvent event) {
        Activity activity = event.getActivity();
        if (activity == null) {
            return;
        }
        String type = activity.getType();
        if (!ISSUE_OPENED.equals(type) && !PR_OPENED.equals(type)) {
            return;
        }
        if (activity.getRepositoryId() == null) {
            return;
        }

        Optional<Repository> repo = repositoryService.findById(activity.getRepositoryId());
        if (repo.isEmpty() || !Repository.OwnerType.USER.name().equals(repo.get().getOwnerType())) {
            return;
        }
        Long ownerId = repo.get().getOwnerId();
        // Do not notify the owner about their own activity.
        if (ownerId == null || ownerId.equals(activity.getActorId())) {
            return;
        }

        String targetType = ISSUE_OPENED.equals(type) ? "issue" : "pull_request";
        notificationService.record(ownerId, type, activity.getMessage(), targetType, activity.getRepositoryId());
    }
}
