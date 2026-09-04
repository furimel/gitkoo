package com.furimeo.gitkoo.git;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.furimeo.gitkoo.web.Languages;

/**
 * The expensive parts of the repository overview, cached against the head commit.
 *
 * <p>{@code languageBytes} walks the entire tree and {@code contributors} walks the
 * entire history. Both are O(repository), and the overview page is the most-visited
 * page there is, so calling them per request would make the whole product measurably
 * slower.
 *
 * <p>The cache key is the head SHA, so a push invalidates it for free and nothing has
 * to be evicted on write. Entries for old SHAs are dropped when the map grows past
 * {@link #MAX_ENTRIES}, which bounds memory without a scheduled sweep.
 */
@Service
public class RepoInsightService {

    /** Roughly one entry per active repository; each holds a handful of small records. */
    private static final int MAX_ENTRIES = 512;

    private static final int MAX_CONTRIBUTORS = 12;

    private final GitService gitService;
    private final Map<String, Insight> cache = new ConcurrentHashMap<>();

    public RepoInsightService(GitService gitService) {
        this.gitService = gitService;
    }

    /**
     * @param storagePath the bare repository
     * @param ref the ref being browsed
     * @return the insight for the commit {@code ref} currently points at, or an empty
     *         one when the ref does not resolve
     */
    public Insight forRef(Path storagePath, String ref) {
        String sha = gitService.resolveRef(storagePath, ref);
        if (sha.isBlank()) {
            return Insight.EMPTY;
        }
        String key = storagePath + "@" + sha;
        Insight hit = cache.get(key);
        if (hit != null) {
            return hit;
        }
        Insight computed = compute(storagePath, sha);
        if (cache.size() >= MAX_ENTRIES) {
            cache.clear();
        }
        cache.put(key, computed);
        return computed;
    }

    private Insight compute(Path storagePath, String sha) {
        Map<String, Long> bytes = gitService.languageBytes(storagePath, sha);
        long total = bytes.values().stream().mapToLong(Long::longValue).sum();

        List<LanguageShare> languages = List.of();
        if (total > 0) {
            LinkedHashMap<String, LanguageShare> shares = new LinkedHashMap<>();
            bytes.forEach((name, size) -> shares.put(name,
                    new LanguageShare(name, Languages.color(name), size, size * 100.0 / total)));
            languages = List.copyOf(shares.values());
        }

        return new Insight(
                languages,
                gitService.contributors(storagePath, sha, MAX_CONTRIBUTORS),
                gitService.sizeBytes(storagePath),
                gitService.licenseName(storagePath, sha),
                gitService.tags(storagePath, 10));
    }

    /** One language's slice of the repository. */
    public record LanguageShare(String name, String color, long bytes, double percent) {

        /** Rounded for display: "62.4%". */
        public String percentLabel() {
            return String.format(java.util.Locale.ROOT, "%.1f%%", percent);
        }
    }

    /** Everything the overview sidebar needs that costs a tree or history walk. */
    public record Insight(List<LanguageShare> languages,
                          List<GitService.Contributor> contributors,
                          long sizeBytes,
                          String license,
                          List<GitService.TagInfo> tags) {

        static final Insight EMPTY = new Insight(List.of(), List.of(), 0L, null, List.of());

        /** "1.2 MB", the way a person reads a repository size. */
        public String sizeLabel() {
            if (sizeBytes < 1024) {
                return sizeBytes + " B";
            }
            if (sizeBytes < 1024 * 1024) {
                return String.format(java.util.Locale.ROOT, "%.0f KB", sizeBytes / 1024.0);
            }
            return String.format(java.util.Locale.ROOT, "%.1f MB", sizeBytes / (1024.0 * 1024.0));
        }
    }
}
