package com.furimeo.gitkoo.auth;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.apache.sshd.common.config.keys.AuthorizedKeyEntry;
import org.apache.sshd.common.config.keys.KeyUtils;
import org.apache.sshd.common.config.keys.PublicKeyEntry;
import org.apache.sshd.common.config.keys.PublicKeyEntryResolver;
import org.springframework.stereotype.Service;

/**
 * Manages user SSH public keys: parsing, fingerprinting, and CRUD (DESIGN.md §8, §43).
 *
 * <p>Public keys are parsed with Apache MINA SSHD and fingerprinted via
 * {@link KeyUtils#getFingerPrint(PublicKey)} so they can be matched against the keys
 * presented during SSH authentication (see {@code GitSshServer}). The database stores
 * the fingerprint, key type, and raw content; Git is never invoked here.
 *
 * @see DESIGN.md §8, §43, §78
 */
@Service
public class SshKeyService {

    private final SshKeyRepository sshKeyRepository;

    public SshKeyService(SshKeyRepository sshKeyRepository) {
        this.sshKeyRepository = sshKeyRepository;
    }

    /** Lists all SSH keys belonging to a user. */
    public List<SshKey> listByUser(Long userId) {
        return sshKeyRepository.findByUserId(userId);
    }

    /** Finds a single SSH key by id. */
    public Optional<SshKey> findById(Long id) {
        return sshKeyRepository.findById(id);
    }

    /**
     * Parses, fingerprints, and stores a public key for a user.
     *
     * @param publicKeyContent a single OpenSSH public key line, e.g.
     *                          {@code ssh-ed25519 AAAA... comment}
     * @throws IllegalArgumentException if the key cannot be parsed or is already registered
     */
    public SshKey addKey(Long userId, String title, String publicKeyContent) {
        String trimmed = publicKeyContent == null ? "" : publicKeyContent.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Public key must not be empty");
        }

        AuthorizedKeyEntry entry;
        try {
            entry = AuthorizedKeyEntry.parseAuthorizedKeyEntry(trimmed);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unrecognized public key format: " + e.getMessage(), e);
        }
        if (entry == null) {
            throw new IllegalArgumentException("Unrecognized public key format");
        }

        PublicKey key;
        try {
            key = entry.resolvePublicKey(null, PublicKeyEntryResolver.IGNORING);
        } catch (IOException | GeneralSecurityException e) {
            throw new IllegalArgumentException("Failed to decode public key: " + e.getMessage(), e);
        }
        if (key == null) {
            throw new IllegalArgumentException("Failed to decode public key");
        }

        String fingerprint = KeyUtils.getFingerPrint(key);
        if (sshKeyRepository.findByFingerprint(fingerprint).isPresent()) {
            throw new IllegalArgumentException("This SSH key is already registered");
        }

        String canonical;
        try {
            canonical = PublicKeyEntry.toString(key);
        } catch (IllegalArgumentException e) {
            canonical = trimmed;
        }

        SshKey sshKey = new SshKey();
        sshKey.setUserId(userId);
        sshKey.setTitle(title);
        sshKey.setFingerprint(fingerprint);
        sshKey.setKeyType(entry.getKeyType());
        sshKey.setPublicKey(canonical);
        sshKey.setContent(trimmed);
        sshKey.setCreatedAt(OffsetDateTime.now());
        return sshKeyRepository.save(sshKey);
    }

    /** Deletes an SSH key by id. */
    public void delete(Long id) {
        sshKeyRepository.deleteById(id);
    }
}
