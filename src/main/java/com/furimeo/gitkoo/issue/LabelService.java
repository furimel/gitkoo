package com.furimeo.gitkoo.issue;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages labels and milestones for a repository (DESIGN.md §18).
 */
@Service
public class LabelService {

    private final LabelRepository labelRepository;
    private final IssueLabelRepository issueLabelRepository;

    public LabelService(LabelRepository labelRepository, IssueLabelRepository issueLabelRepository) {
        this.labelRepository = labelRepository;
        this.issueLabelRepository = issueLabelRepository;
    }

    @Transactional
    public Label createLabel(Long repositoryId, String name, String color, String description) {
        if (labelRepository.existsByRepositoryIdAndName(repositoryId, name)) {
            throw new IllegalArgumentException("Label '" + name + "' already exists");
        }
        Label label = new Label();
        label.setRepositoryId(repositoryId);
        label.setName(name);
        label.setColor(color != null ? color : "#0969da");
        label.setDescription(description);
        return labelRepository.save(label);
    }

    public List<Label> listLabels(Long repositoryId) {
        return labelRepository.findByRepositoryId(repositoryId);
    }

    @Transactional
    public void addLabelToIssue(Long issueId, Long labelId) {
        if (!issueLabelRepository.existsByIssueIdAndLabelId(issueId, labelId)) {
            IssueLabel il = new IssueLabel();
            il.setIssueId(issueId);
            il.setLabelId(labelId);
            issueLabelRepository.save(il);
        }
    }

    @Transactional
    public void removeLabelFromIssue(Long issueId, Long labelId) {
        issueLabelRepository.deleteByIssueIdAndLabelId(issueId, labelId);
    }

    public List<IssueLabel> getIssueLabels(Long issueId) {
        return issueLabelRepository.findByIssueId(issueId);
    }

    /**
     * Labels attached to each of {@code issueIds}, for rendering a list of issues.
     *
     * <p>Two queries total regardless of how many issues there are, rather than one
     * per row. Issues with no labels are absent from the map.
     */
    public java.util.Map<Long, List<Label>> labelsByIssue(Long repositoryId,
                                                          java.util.Collection<Long> issueIds) {
        if (issueIds == null || issueIds.isEmpty()) {
            return java.util.Map.of();
        }
        java.util.Map<Long, Label> byId = new java.util.HashMap<>();
        for (Label label : labelRepository.findByRepositoryId(repositoryId)) {
            byId.put(label.getId(), label);
        }

        java.util.Map<Long, List<Label>> result = new java.util.HashMap<>();
        for (IssueLabel link : issueLabelRepository.findByIssueIdIn(issueIds)) {
            Label label = byId.get(link.getLabelId());
            if (label != null) {
                result.computeIfAbsent(link.getIssueId(), k -> new java.util.ArrayList<>()).add(label);
            }
        }
        return result;
    }
}
