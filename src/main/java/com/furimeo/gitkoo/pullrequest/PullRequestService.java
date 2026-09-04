package com.furimeo.gitkoo.pullrequest;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.furimeo.gitkoo.activity.ActivityService;
import com.furimeo.gitkoo.git.GitService;
import com.furimeo.gitkoo.issue.IssueService;

/**
 * Creates, reviews, and merges pull requests (DESIGN.md §15, §16, §17).
 *
 * @see DESIGN.md §15, §16, §17, §46
 */
@Service
public class PullRequestService {

    private final PullRequestRepository prRepository;
    private final PullRequestReviewRepository reviewRepository;
    private final GitService gitService;
    private final ActivityService activityService;
    private final IssueService issueService;

    public PullRequestService(PullRequestRepository prRepository,
                             PullRequestReviewRepository reviewRepository,
                             GitService gitService,
                             ActivityService activityService,
                             IssueService issueService) {
        this.prRepository = prRepository;
        this.reviewRepository = reviewRepository;
        this.gitService = gitService;
        this.activityService = activityService;
        this.issueService = issueService;
    }

    @Transactional
    public PullRequest create(Long repositoryId, String title, String body, Long authorId,
                             String sourceBranch, String targetBranch) {
        int nextNumber = prRepository.maxNumber(repositoryId) + 1;
        OffsetDateTime now = OffsetDateTime.now();
        PullRequest pr = new PullRequest();
        pr.setRepositoryId(repositoryId);
        pr.setNumber(nextNumber);
        pr.setTitle(title);
        pr.setBody(body);
        pr.setAuthorId(authorId);
        pr.setSourceBranch(sourceBranch);
        pr.setTargetBranch(targetBranch);
        pr.setStatus(PullRequest.Status.OPEN.name());
        pr.setCreatedAt(now);
        pr.setUpdatedAt(now);
        pr = prRepository.save(pr);
        activityService.record(repositoryId, authorId, "PR_OPENED",
                "opened PR #" + pr.getNumber() + ": " + title);
        return pr;
    }

    @Transactional
    public PullRequestReview addReview(Long prId, Long reviewerId, String state, String body) {
        PullRequestReview review = new PullRequestReview();
        review.setPullRequestId(prId);
        review.setReviewerId(reviewerId);
        review.setState(state);
        review.setBody(body);
        review.setCreatedAt(OffsetDateTime.now());
        return reviewRepository.save(review);
    }

    /**
     * Merges a PR via git merge --no-ff (DESIGN.md §17). MVP supports merge-commit only;
     * squash/rebase can be added later.
     *
     * @param storagePath the repository's bare git path
     * @return the merge commit SHA, or null if the merge failed
     */
    @Transactional
    public String merge(Long prId, Path storagePath) {
        PullRequest pr = prRepository.findById(prId).orElseThrow();
        if (!PullRequest.Status.OPEN.name().equals(pr.getStatus())) {
            throw new IllegalStateException("Cannot merge a non-open PR");
        }

        // Merge via a temporary non-bare clone so we can checkout the target branch
        // and merge correctly (bare repos merge into HEAD only, ignoring target).
        String mergeMsg = "Merge PR #" + pr.getNumber() + ": " + pr.getTitle();
        Path tempDir;
        try {
            tempDir = java.nio.file.Files.createTempDirectory("gitkoo-merge");
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Failed to create temp dir for merge", e);
        }

        try {
            gitService.run(tempDir, "clone", storagePath.toString(), tempDir.toString());
            gitService.run(tempDir, "checkout", pr.getTargetBranch());
            GitService.GitResult mergeResult = gitService.run(tempDir, "merge", "--no-ff",
                    "-m", mergeMsg, pr.getSourceBranch());
            if (!mergeResult.success()) {
                return null;
            }
            // Push the merged target branch back to the bare repo.
            gitService.run(tempDir, "push", "origin", pr.getTargetBranch());
        } finally {
            try {
                deleteRecursively(tempDir);
            } catch (java.io.IOException e) {
                // best-effort cleanup
            }
        }

        // Resolve the new HEAD on the target branch in the bare repo.
        String sha = gitService.resolveRef(storagePath,
                "refs/heads/" + pr.getTargetBranch());

        OffsetDateTime now = OffsetDateTime.now();
        pr.setStatus(PullRequest.Status.MERGED.name());
        pr.setMergeCommitSha(sha);
        pr.setMergedAt(now);
        pr.setUpdatedAt(now);
        prRepository.save(pr);
        activityService.record(pr.getRepositoryId(), pr.getAuthorId(), "PR_MERGED",
                "merged PR #" + pr.getNumber());

        // Auto-close issues referenced by "fixes #NN" / "closes #NN" (DESIGN.md §20).
        if (pr.getBody() != null) {
            issueService.autoCloseFromText(pr.getBody(), pr.getRepositoryId());
        }
        return sha;
    }

    private static void deleteRecursively(Path path) throws java.io.IOException {
        if (java.nio.file.Files.isDirectory(path)) {
            try (var entries = java.nio.file.Files.list(path)) {
                entries.forEach(p -> {
                    try { deleteRecursively(p); } catch (java.io.IOException ignored) {}
                });
            }
        }
        java.nio.file.Files.deleteIfExists(path);
    }

    @Transactional
    public void close(Long prId) {
        PullRequest pr = prRepository.findById(prId).orElseThrow();
        pr.setStatus(PullRequest.Status.CLOSED.name());
        pr.setUpdatedAt(OffsetDateTime.now());
        pr.setClosedAt(OffsetDateTime.now());
        prRepository.save(pr);
        activityService.record(pr.getRepositoryId(), pr.getAuthorId(), "PR_CLOSED",
                "closed PR #" + pr.getNumber());
    }

    public List<PullRequest> listByRepository(Long repositoryId) {
        return prRepository.findByRepositoryIdOrderByNumberDesc(repositoryId);
    }

    public PullRequest findByRepositoryAndNumber(Long repositoryId, Integer number) {
        return prRepository.findByRepositoryIdAndNumber(repositoryId, number).orElse(null);
    }

    public List<PullRequestReview> listReviews(Long prId) {
        return reviewRepository.findByPullRequestIdOrderByCreatedAtAsc(prId);
    }
}
