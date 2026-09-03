package com.furimeo.gitkoo.issue;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Manages milestones for a repository (DESIGN.md §18). */
@Service
public class MilestoneService {

    private final MilestoneRepository milestoneRepository;

    public MilestoneService(MilestoneRepository milestoneRepository) {
        this.milestoneRepository = milestoneRepository;
    }

    @Transactional
    public Milestone create(Long repositoryId, String title, String description, OffsetDateTime dueDate) {
        OffsetDateTime now = OffsetDateTime.now();
        Milestone m = new Milestone();
        m.setRepositoryId(repositoryId);
        m.setTitle(title);
        m.setDescription(description);
        m.setDueDate(dueDate);
        m.setStatus(Milestone.Status.OPEN.name());
        m.setCreatedAt(now);
        m.setUpdatedAt(now);
        return milestoneRepository.save(m);
    }

    public List<Milestone> listByRepository(Long repositoryId) {
        return milestoneRepository.findByRepositoryId(repositoryId);
    }
}
