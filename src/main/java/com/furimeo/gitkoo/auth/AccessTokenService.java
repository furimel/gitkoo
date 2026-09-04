package com.furimeo.gitkoo.auth;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Generates and validates personal access tokens (see docs/).
 *
 * <p>A token has the form {@code gitkoo_<random>}. The random part is 32 bytes hex-encoded.
 * Only a hash of the token is stored (BCrypt), so a leaked database does not expose usable
 * tokens. The raw token is shown to the user exactly once at creation time.
 */
@Service
public class AccessTokenService {

    /** Token prefix so tokens are visually identifiable and greppable. */
    public static final String TOKEN_PREFIX = "gitkoo_";

    private static final SecureRandom RNG = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    private final AccessTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;

    public AccessTokenService(AccessTokenRepository tokenRepository, PasswordEncoder passwordEncoder) {
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Creates a new token for a user. Returns the raw token (shown once). The hash is
     * persisted; the caller must NOT store the raw token.
     */
    public CreateTokenResult create(Long userId, String name, String scopes) {
        String rawToken = generateRawToken();
        String hash = passwordEncoder.encode(rawToken);

        AccessToken token = new AccessToken();
        token.setUserId(userId);
        token.setName(name);
        token.setTokenHash(hash);
        token.setScopes(scopes);
        OffsetDateTime now = OffsetDateTime.now();
        token.setCreatedAt(now);
        token = tokenRepository.save(token);

        return new CreateTokenResult(token.getId(), rawToken);
    }

    /**
     * Looks up a raw token by hashing candidate tokens and matching. Since the stored hash
     * is BCrypt (non-reversible), we cannot query by hash directly; instead the API filter
     * must iterate. For the MVP scale (see docs/) this is fine, and tokens can later be
     * looked up via a non-secret index if needed.
     *
     * <p>Returns the token plus the resolved user when the raw token matches a stored hash.
     */
    public Optional<ResolvedToken> resolve(String rawToken) {
        if (rawToken == null || !rawToken.startsWith(TOKEN_PREFIX)) {
            return Optional.empty();
        }
        for (AccessToken stored : tokenRepository.findAll()) {
            if (passwordEncoder.matches(rawToken, stored.getTokenHash())) {
                // Update last-used timestamp (best-effort; ignore failures).
                try {
                    stored.setLastUsedAt(OffsetDateTime.now());
                    tokenRepository.save(stored);
                } catch (Exception ignored) {
                    // last-used update is non-critical
                }
                return Optional.of(new ResolvedToken(stored));
            }
        }
        return Optional.empty();
    }

    static String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RNG.nextBytes(bytes);
        return TOKEN_PREFIX + HexFormat.of().formatHex(bytes);
    }

    /** Result of token creation: the persisted id and the one-time raw token. */
    public record CreateTokenResult(Long tokenId, String rawToken) {
    }

    /** A token resolved from a raw value, with its stored metadata. */
    public record ResolvedToken(AccessToken token) {
        public Long userId() {
            return token.getUserId();
        }

        public String scopes() {
            return token.getScopes();
        }
    }
}
