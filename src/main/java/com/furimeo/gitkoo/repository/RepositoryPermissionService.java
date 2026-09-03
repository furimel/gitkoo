package com.furimeo.gitkoo.repository;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.furimeo.gitkoo.auth.User;
import com.furimeo.gitkoo.team.TeamMemberRepository;
import com.furimeo.gitkoo.team.TeamRepository;

/**
 * Centralizes repository permission checks (DESIGN.md §22, §44).
 *
 * <p>Used by both web controllers and git transport (HTTP + SSH) so permission logic
 * is not scattered across controllers (§44). Returns the highest permission a user has
 * on a repository.
 *
 * <p>Mapping (§22):
 * <ul>
 *   <li>Repository owner → ADMIN</li>
 *   <li>Team OWNER → ADMIN, MAINTAINER → WRITE, MEMBER → READ</li>
 *   <li>repository_members explicit → READ|WRITE|ADMIN</li>
 *   <li>PUBLIC → READ for all; INTERNAL → READ for logged-in; PRIVATE → only grants</li>
 * </ul>
 *
 * @see DESIGN.md §22, §44
 */
@Service
public class RepositoryPermissionService {

    public enum Permission { NONE, READ, WRITE, ADMIN }

    private final RepositoryRepository repositoryRepository;
    private final RepositoryMemberRepository repositoryMemberRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;

    public RepositoryPermissionService(RepositoryRepository repositoryRepository,
                                      RepositoryMemberRepository repositoryMemberRepository,
                                      TeamRepository teamRepository,
                                      TeamMemberRepository teamMemberRepository) {
        this.repositoryRepository = repositoryRepository;
        this.repositoryMemberRepository = repositoryMemberRepository;
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
    }

    /** Returns the permission level a user has on a repository. */
    public Permission permission(User user, Repository repo) {
        if (repo == null) return Permission.NONE;

        // Owner of a USER-owned repo is always ADMIN.
        if (user != null && "USER".equals(repo.getOwnerType())
                && user.getId().equals(repo.getOwnerId())) {
            return Permission.ADMIN;
        }

        Permission explicit = explicitMemberPermission(user, repo);
        Permission teamPerm = teamPermission(user, repo);
        Permission best = max(explicit, teamPerm);

        // If we have a grant from membership, return it.
        if (best != Permission.NONE) {
            return best;
        }

        // Fall back to visibility-based access.
        return visibilityPermission(user, repo);
    }

    /** Checks if the user has at least the required permission. */
    public boolean hasPermission(User user, Repository repo, Permission required) {
        return permission(user, repo).ordinal() >= required.ordinal();
    }

    private Permission explicitMemberPermission(User user, Repository repo) {
        if (user == null) return Permission.NONE;
        return repositoryMemberRepository.findByRepositoryIdAndUserId(repo.getId(), user.getId())
                .map(m -> Permission.valueOf(m.getPermission()))
                .orElse(Permission.NONE);
    }

    private Permission teamPermission(User user, Repository repo) {
        if (user == null || !"TEAM".equals(repo.getOwnerType())) return Permission.NONE;
        // Find the user's role in the team that owns this repo.
        return teamMemberRepository.findByTeamIdAndUserId(repo.getOwnerId(), user.getId())
                .map(m -> switch (m.getRole()) {
                    case "OWNER" -> Permission.ADMIN;
                    case "MAINTAINER" -> Permission.WRITE;
                    default -> Permission.READ;
                })
                .orElse(Permission.NONE);
    }

    private Permission visibilityPermission(User user, Repository repo) {
        String vis = repo.getVisibility();
        if (vis == null) vis = "PUBLIC";
        return switch (vis) {
            case "PUBLIC" -> Permission.READ;
            case "INTERNAL" -> user != null ? Permission.READ : Permission.NONE;
            default -> Permission.NONE; // PRIVATE
        };
    }

    private Permission max(Permission a, Permission b) {
        return a.ordinal() >= b.ordinal() ? a : b;
    }
}
