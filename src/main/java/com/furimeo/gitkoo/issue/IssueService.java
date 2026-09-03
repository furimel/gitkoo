package com.furimeo.gitkoo.issue;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates issues, adds comments, and closes issues (DESIGN.md §18, §20).
 *
 * @see DESIGN.md §18, §20, §116
 */
@Service
public class IssueService {

    private final IssueRepository issueRepository;
    private final IssueCommentRepository commentRepository;

    public IssueService(IssueRepository issueRepository, IssueCommentRepository commentRepository) {
        this.issueRepository = issueRepository;
        this.commentRepository = commentRepository;
    }

    /** Creates a new issue with a per-repository sequence number (DESIGN.md §116). */
    @Transactional
    public Issue create(Long repositoryId, String title, String body, Long authorId) {
        int nextNumber = issueRepository.maxNumber(repositoryId) + 1;
        OffsetDateTime now = OffsetDateTime.now();
        Issue issue = new Issue();
        issue.setRepositoryId(repositoryId);
        issue.setNumber(nextNumber);
        issue.setTitle(title);
        issue.setBody(body);
        issue.setAuthorId(authorId);
        issue.setStatus(Issue.Status.OPEN.name());
        issue.setCreatedAt(now);
        issue.setUpdatedAt(now);
        return issueRepository.save(issue);
    }

    /** Adds a comment to an issue. */
    @Transactional
    public IssueComment addComment(Long issueId, String body, Long authorId) {
        OffsetDateTime now = OffsetDateTime.now();
        IssueComment comment = new IssueComment();
        comment.setIssueId(issueId);
        comment.setAuthorId(authorId);
        comment.setBody(body);
        comment.setCreatedAt(now);
        comment.setUpdatedAt(now);
        comment = commentRepository.save(comment);
        // Update issue's updatedAt for sorting.
        Issue issue = issueRepository.findById(issueId).orElseThrow();
        issue.setUpdatedAt(now);
        issueRepository.save(issue);
        return comment;
    }

    /** Closes an issue by ID. */
    @Transactional
    public void close(Long issueId) {
        Issue issue = issueRepository.findById(issueId).orElseThrow();
        issue.setStatus(Issue.Status.CLOSED.name());
        issue.setUpdatedAt(OffsetDateTime.now());
        issue.setClosedAt(OffsetDateTime.now());
        issueRepository.save(issue);
    }

    /** Reopens a closed issue. */
    @Transactional
    public void reopen(Long issueId) {
        Issue issue = issueRepository.findById(issueId).orElseThrow();
        issue.setStatus(Issue.Status.OPEN.name());
        issue.setUpdatedAt(OffsetDateTime.now());
        issue.setClosedAt(null);
        issueRepository.save(issue);
    }

    public List<Issue> listByRepository(Long repositoryId) {
        return issueRepository.findByRepositoryIdOrderByNumberDesc(repositoryId);
    }

    public Issue findByRepositoryAndNumber(Long repositoryId, Integer number) {
        return issueRepository.findByRepositoryIdAndNumber(repositoryId, number).orElse(null);
    }

    public List<IssueComment> listComments(Long issueId) {
        return commentRepository.findByIssueIdOrderByCreatedAtAsc(issueId);
    }

    /**
     * Auto-closes issues referenced by "fixes #NN" / "closes #NN" / "resolves #NN"
     * in the given text (DESIGN.md §20). Best-effort: silently skips non-existent issues.
     */
    @Transactional
    public void autoCloseFromText(String text, Long repositoryId) {
        for (int number : IssueLinker.findAutoClose(text)) {
            Issue issue = findByRepositoryAndNumber(repositoryId, number);
            if (issue != null && Issue.Status.OPEN.name().equals(issue.getStatus())) {
                close(issue.getId());
            }
        }
    }
}
