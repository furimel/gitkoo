package com.furimeo.gitkoo.web;

import java.util.List;

/**
 * One slice of a list, with just enough context for the pagination control.
 *
 * <p>Lists are paged in memory rather than in SQL. That is deliberate for now: the
 * repositories return plain {@code List}s and a self-hosted instance holds tens of
 * issues per repository, not millions. When a list outgrows that, this record is
 * the seam to push {@code LIMIT}/{@code OFFSET} down into the query behind.
 *
 * @param items the rows on this page
 * @param page 1-based page number
 * @param size rows per page
 * @param total total rows across all pages
 */
public record Page<T>(List<T> items, int page, int size, int total) {

    /** Default rows per page, matching GitHub's issue and pull request lists. */
    public static final int DEFAULT_SIZE = 25;

    /**
     * Slices {@code all} for the requested page.
     *
     * <p>An out-of-range page is clamped rather than rejected, so a stale bookmark
     * lands on the last page instead of an error.
     */
    public static <T> Page<T> of(List<T> all, Integer requestedPage, int size) {
        int total = all.size();
        int pages = Math.max(1, (int) Math.ceil((double) total / size));
        int page = requestedPage == null ? 1 : Math.min(Math.max(requestedPage, 1), pages);
        int from = Math.min((page - 1) * size, total);
        int to = Math.min(from + size, total);
        return new Page<>(all.subList(from, to), page, size, total);
    }

    public static <T> Page<T> of(List<T> all, Integer requestedPage) {
        return of(all, requestedPage, DEFAULT_SIZE);
    }

    public int totalPages() {
        return Math.max(1, (int) Math.ceil((double) total / size));
    }

    public boolean hasPrevious() {
        return page > 1;
    }

    public boolean hasNext() {
        return page < totalPages();
    }

    public int previousPage() {
        return Math.max(1, page - 1);
    }

    public int nextPage() {
        return Math.min(totalPages(), page + 1);
    }

    /** True when everything fits on one page, so the control can be hidden entirely. */
    public boolean single() {
        return totalPages() <= 1;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }
}
