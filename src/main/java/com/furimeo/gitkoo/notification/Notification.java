package com.furimeo.gitkoo.notification;

import java.time.OffsetDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * A notification for a single user (DESIGN.md §116).
 *
 * <p>Notifications are user-oriented: they tell someone that something happened that is
 * relevant to them (a new issue on their repo, a review on their PR). The optional
 * {@code targetType}/{@code targetId} link back to the object that triggered the notice.
 *
 * @see DESIGN.md §116
 */
@Table("notifications")
public class Notification {

    @Id
    private Long id;
    private Long userId;
    private String type;
    private String message;
    private String targetType;
    private Long targetId;
    private boolean read;
    private OffsetDateTime createdAt;

    public Notification() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
