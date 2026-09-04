package com.furimeo.gitkoo.repository;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Stars, watchers and topics (DESIGN.md §11). */
@Service
public class SocialService {

    private final StarRepository stars;
    private final WatcherRepository watchers;
    private final TopicRepository topics;

    public SocialService(StarRepository stars, WatcherRepository watchers, TopicRepository topics) {
        this.stars = stars;
        this.watchers = watchers;
        this.topics = topics;
    }

    // ── stars ───────────────────────────────────────────────────────────

    public int starCount(Long repositoryId) {
        return stars.countByRepositoryId(repositoryId);
    }

    public boolean isStarredBy(Long repositoryId, Long userId) {
        return userId != null && stars.existsByRepositoryIdAndUserId(repositoryId, userId);
    }

    /**
     * Adds or removes a star.
     *
     * @return true when the repository is starred afterwards
     */
    @Transactional
    public boolean toggleStar(Long repositoryId, Long userId) {
        if (stars.existsByRepositoryIdAndUserId(repositoryId, userId)) {
            stars.deleteStar(repositoryId, userId);
            return false;
        }
        stars.save(new RepositoryStar(repositoryId, userId, OffsetDateTime.now()));
        return true;
    }

    public List<Long> starredRepositoryIds(Long userId) {
        return stars.starredRepositoryIds(userId);
    }

    // ── watchers ────────────────────────────────────────────────────────

    public int watcherCount(Long repositoryId) {
        return watchers.countByRepositoryId(repositoryId);
    }

    public boolean isWatchedBy(Long repositoryId, Long userId) {
        return userId != null && watchers.existsByRepositoryIdAndUserId(repositoryId, userId);
    }

    /**
     * Subscribes to or unsubscribes from a repository.
     *
     * @return true when the repository is watched afterwards
     */
    @Transactional
    public boolean toggleWatch(Long repositoryId, Long userId) {
        if (watchers.existsByRepositoryIdAndUserId(repositoryId, userId)) {
            watchers.deleteWatch(repositoryId, userId);
            return false;
        }
        watchers.save(new RepositoryWatcher(repositoryId, userId, OffsetDateTime.now()));
        return true;
    }

    public List<Long> watcherIds(Long repositoryId) {
        return watchers.watcherIds(repositoryId);
    }

    // ── topics ──────────────────────────────────────────────────────────

    public List<String> topics(Long repositoryId) {
        return topics.findByRepositoryIdOrderByTopic(repositoryId).stream()
                .map(RepositoryTopic::getTopic)
                .toList();
    }

    /**
     * Replaces a repository's topics with the given comma or space separated list.
     *
     * <p>Normalised the way GitHub does it: lowercase, spaces to hyphens, at most 20.
     */
    @Transactional
    public void setTopics(Long repositoryId, String raw) {
        topics.deleteAllFor(repositoryId);
        if (raw == null || raw.isBlank()) {
            return;
        }
        java.util.LinkedHashSet<String> seen = new java.util.LinkedHashSet<>();
        for (String part : raw.split("[,\\s]+")) {
            String topic = part.strip().toLowerCase(java.util.Locale.ROOT).replace(' ', '-');
            if (!topic.isEmpty() && topic.length() <= 35) {
                seen.add(topic);
            }
            if (seen.size() >= 20) {
                break;
            }
        }
        seen.forEach(topic -> topics.save(new RepositoryTopic(repositoryId, topic)));
    }

    /** Topics for many repositories in one query, keyed by repository id. */
    public Map<Long, List<String>> topicsByRepository(List<Long> repositoryIds) {
        if (repositoryIds == null || repositoryIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<String>> byRepo = new HashMap<>();
        for (RepositoryTopic t : topics.findAllFor(repositoryIds)) {
            byRepo.computeIfAbsent(t.getRepositoryId(), k -> new java.util.ArrayList<>())
                    .add(t.getTopic());
        }
        return byRepo;
    }
}
