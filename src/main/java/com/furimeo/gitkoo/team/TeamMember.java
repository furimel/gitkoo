package com.furimeo.gitkoo.team;

import java.time.OffsetDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/** A user's membership in a team, with a role (DESIGN.md §21, §22). */
@Table("team_members")
public class TeamMember {

    public enum Role { OWNER, MAINTAINER, MEMBER }

    @Id
    private Long id;
    private Long teamId;
    private Long userId;
    private String role;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public TeamMember() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTeamId() { return teamId; }
    public void setTeamId(Long v) { this.teamId = v; }
    public Long getUserId() { return userId; }
    public void setUserId(Long v) { this.userId = v; }
    public String getRole() { return role; }
    public void setRole(String v) { this.role = v; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime v) { this.createdAt = v; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime v) { this.updatedAt = v; }
}
