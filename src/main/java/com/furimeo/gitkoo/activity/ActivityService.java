package com.furimeo.gitkoo.activity;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

/**
 * Records and lists human-readable activity events (DESIGN.md §23).
 *
 * <p>Activity is work-oriented: pushed commits, opened PRs, closed issues.
 * Not a social feed (no trending, followers, or viral posts, DESIGN.md §55).
 */
@Service
public class ActivityService {

    private final ActivityRepository repository;

    public ActivityService(ActivityRepository repository) {
        this.repository = repository;
    }

    /** Records an activity event. */
    public Activity record(Long repositoryId, Long actorId, String type, String message) {
        Activity activity = new Activity();
        activity.setRepositoryId(repositoryId);
        activity.setActorId(actorId);
        activity.setType(type);
        activity.setMessage(message);
        activity.setCreatedAt(OffsetDateTime.now());
        return repository.save(activity);
    }

    /** Lists recent activities for a repository. */
    public List<Activity> listByRepository(Long repositoryId) {
        return repository.findByRepositoryIdOrderByCreatedAtDesc(repositoryId);
    }

    /** Lists the 20 most recent activities across all repositories. */
    public List<Activity> listRecent() {
        return repository.findTop20ByOrderByCreatedAtDesc();
    }
}
