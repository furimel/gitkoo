package com.furimeo.gitkoo.auth;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

/** Repository for SSH public keys (DESIGN.md §8, §43). */
public interface SshKeyRepository extends CrudRepository<SshKey, Long> {
    List<SshKey> findByUserId(Long userId);
    java.util.Optional<SshKey> findByFingerprint(String fingerprint);
}
