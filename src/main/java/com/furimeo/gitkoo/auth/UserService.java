package com.furimeo.gitkoo.auth;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Handles user creation, lookup, and first-run admin detection.
 *
 * @see DESIGN.md §10, §43, §68
 */
@Service
public class UserService {

    /** Username constraints: lowercase alphanumerics and hyphens, 1–39 chars (design §10). */
    static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9-]{0,38}$");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Returns true when no admin user exists yet - the instance needs first-run setup
     * (DESIGN.md §68).
     */
    public boolean needsSetup() {
        return !userRepository.existsAdmin();
    }

    /**
     * Creates the first administrator account during first-run setup.
     *
     * @throws IllegalArgumentException if username/email is invalid or already taken
     * @throws IllegalStateException if an admin already exists (setup already done)
     */
    public User createAdministrator(String username, String email, String password) {
        if (!needsSetup()) {
            throw new IllegalStateException("Setup already completed");
        }
        return doCreateUser(username, email, password, true);
    }

    /**
     * Creates a regular (non-admin) user. Used by admin UI and tests.
     * Does not require setup to be in progress.
     */
    public User createUser(String username, String email, String password) {
        return doCreateUser(username, email, password, false);
    }

    private User doCreateUser(String username, String email, String password, boolean admin) {
        validateUsername(username);
        validateEmail(email);
        validatePassword(password);

        User user = new User();
        user.setUsername(username.toLowerCase());
        user.setEmail(email.toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setDisplayName(username);
        user.setStatus("ACTIVE");
        user.setAdmin(admin);
        OffsetDateTime now = OffsetDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        return userRepository.save(user);
    }

    static void validateUsername(String username) {
        if (username == null || !USERNAME_PATTERN.matcher(username).matches()) {
            throw new IllegalArgumentException(
                    "Username must be 1-39 characters of lowercase letters, digits, and hyphens");
        }
    }

    static void validateEmail(String email) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            throw new IllegalArgumentException("A valid email is required");
        }
    }

    static void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }
    }
}
