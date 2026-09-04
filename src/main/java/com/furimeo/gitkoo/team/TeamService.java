package com.furimeo.gitkoo.team;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.furimeo.gitkoo.activity.AuditService;

/**
 * Creates teams and manages membership (DESIGN.md §21).
 */
@Service
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final AuditService auditService;

    public TeamService(TeamRepository teamRepository, TeamMemberRepository teamMemberRepository,
                       AuditService auditService) {
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.auditService = auditService;
    }

    @Transactional
    public Team create(String name, String displayName, String description, Long creatorId) {
        if (teamRepository.existsByName(name)) {
            throw new IllegalArgumentException("Team '" + name + "' already exists");
        }
        OffsetDateTime now = OffsetDateTime.now();
        Team team = new Team();
        team.setName(name);
        team.setDisplayName(displayName != null ? displayName : name);
        team.setDescription(description);
        team.setCreatedAt(now);
        team.setUpdatedAt(now);
        team = teamRepository.save(team);

        // Creator becomes OWNER.
        addMember(team.getId(), creatorId, TeamMember.Role.OWNER.name());
        auditService.record(creatorId, "TEAM_CREATED", "team", team.getId(), null);
        return team;
    }

    @Transactional
    public TeamMember addMember(Long teamId, Long userId, String role) {
        if (teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)) {
            throw new IllegalArgumentException("User is already a member of this team");
        }
        OffsetDateTime now = OffsetDateTime.now();
        TeamMember member = new TeamMember();
        member.setTeamId(teamId);
        member.setUserId(userId);
        member.setRole(role);
        member.setCreatedAt(now);
        member.setUpdatedAt(now);
        teamMemberRepository.save(member);
        auditService.record(userId, "TEAM_MEMBER_ADDED", "team", teamId, null);
        return member;
    }

    @Transactional
    public void removeMember(Long teamId, Long userId) {
        teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                .ifPresent(teamMemberRepository::delete);
    }

    public Optional<Team> findByName(String name) {
        return teamRepository.findByName(name);
    }

    public Optional<Team> findById(Long id) {
        return teamRepository.findById(id);
    }

    public List<TeamMember> listMembers(Long teamId) {
        return teamMemberRepository.findByTeamId(teamId);
    }

    /** Returns the role a user has in a team, or empty if not a member. */
    public Optional<String> getRole(Long teamId, Long userId) {
        return teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                .map(TeamMember::getRole);
    }
}
