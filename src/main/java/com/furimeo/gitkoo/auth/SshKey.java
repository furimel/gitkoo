package com.furimeo.gitkoo.auth;

import java.time.OffsetDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/** An SSH public key registered by a user for Git access over SSH (DESIGN.md §8, §43). */
@Table("ssh_keys")
public class SshKey {

    @Id
    private Long id;
    private Long userId;
    private String title;
    private String fingerprint;
    private String keyType;
    private String publicKey;
    private String content;
    private OffsetDateTime createdAt;

    public SshKey() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long v) { this.userId = v; }
    public String getTitle() { return title; }
    public void setTitle(String v) { this.title = v; }
    public String getFingerprint() { return fingerprint; }
    public void setFingerprint(String v) { this.fingerprint = v; }
    public String getKeyType() { return keyType; }
    public void setKeyType(String v) { this.keyType = v; }
    public String getPublicKey() { return publicKey; }
    public void setPublicKey(String v) { this.publicKey = v; }
    public String getContent() { return content; }
    public void setContent(String v) { this.content = v; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime v) { this.createdAt = v; }
}
