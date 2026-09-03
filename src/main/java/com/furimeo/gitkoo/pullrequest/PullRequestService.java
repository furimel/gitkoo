package com.furimeo.gitkoo.pullrequest;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.furimeo.gitkoo.git.GitService;

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

    public PullRequestService(PullRequestRepository prRepository,
                             PullRequestReviewRepository reviewRepository,
                             GitService gitService) {
        this.prRepository = prRepository;
        this.reviewRepository = reviewRepository;
        this.gitService = gitService;
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
        return prRepository.save(pr);
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

        // git merge via the CLI in the bare repo.
        GitService.GitResult result = gitService.run(storagePath,
                "merge", "--no-ff", "-m",
                "Merge PR #" + pr.getNumber() + ": " + pr.getTitle(),
                pr.getSourceBranch());

        if (!result.success()) {
            return null;
        }

        // Resolve the new HEAD on the target branch.
        String sha = gitService.resolveRef(storagePath,
                "refs/heads/" + pr.getTargetBranch());

        OffsetDateTime now = OffsetDateTime.now();
        pr.setStatus(PullRequest.Status.MERGED.name());
        pr.setMergeCommitSha(sha);
        pr.setMergedAt(now);
        pr.setUpdatedAt(now);
        prRepository.save(pr);
        return sha;
    }

    @Transactional
    public void close(Long prId) {
        PullRequest pr = prRepository.findById(prId).orElseThrow();
        pr.setStatus(PullRequest.Status.CLOSED.name());
        pr.setUpdatedAt(OffsetDateTime.now());
        pr.setClosedAt(OffsetDateTime.now());
        prRepository.save(pr);
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
