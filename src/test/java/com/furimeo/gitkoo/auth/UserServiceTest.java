package com.furimeo.gitkoo.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration test for the first-run setup and user creation flow (DESIGN.md §68, §94).
 *
 * <p>Uses the real SQLite database (migrated on startup) \u2014 no mocks. Each test is
 * transactional and rolls back, so tests do not see each other's data. Tests that need an
 * admin present create one explicitly at the start.
 */
@SpringBootTest
@Transactional
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void needsSetupTrueWhenNoAdmin() {
        assertThat(userService.needsSetup()).isTrue();
    }

    @Test
    void createAdministratorCreatesAdminUser() {
        User admin = userService.createAdministrator("alice", "alice@example.com", "password123");

        assertThat(admin.getId()).isNotNull();
        assertThat(admin.getUsername()).isEqualTo("alice");
        assertThat(admin.getEmail()).isEqualTo("alice@example.com");
        assertThat(admin.isAdmin()).isTrue();
        assertThat(admin.getStatus()).isEqualTo("ACTIVE");
        assertThat(admin.getPasswordHash()).isNotEqualTo("password123"); // hashed
        assertThat(admin.getCreatedAt()).isNotNull();

        Optional<User> found = userRepository.findByUsername("alice");
        assertThat(found).isPresent();
        assertThat(found.get().isAdmin()).isTrue();

        assertThat(userService.needsSetup()).isFalse();
    }

    @Test
    void createAdministratorThrowsWhenSetupAlreadyDone() {
        userService.createAdministrator("admin", "admin@example.com", "password123");
        assertThatThrownBy(() -> userService.createAdministrator("second", "second@example.com", "password123"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Setup already completed");
    }

    @Test
    void createAdministratorThrowsOnDuplicateUsername() {
        userService.createAdministrator("bob", "bob@example.com", "password123");
        // After an admin exists, createAdministrator throws IllegalStateException, not
        // a constraint violation, because the setup guard fires first. So we exercise the
        // uniqueness constraint by inserting a second user via the repository directly.
        User second = new User();
        second.setUsername("bob");
        second.setEmail("bob2@example.com");
        second.setPasswordHash("hash");
        second.setStatus("ACTIVE");
        second.setCreatedAt(java.time.OffsetDateTime.now());
        second.setUpdatedAt(java.time.OffsetDateTime.now());
        assertThatThrownBy(() -> userRepository.save(second))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
    }

    @Test
    void validateUsernameRejectsUppercase() {
        assertThatThrownBy(() -> UserService.validateUsername("Alice"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lowercase");
    }

    @Test
    void validateUsernameRejectsTooLong() {
        assertThatThrownBy(() -> UserService.validateUsername("a".repeat(40)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validateUsernameAcceptsValid() {
        UserService.validateUsername("minh");
        UserService.validateUsername("a1-b2");
    }

    @Test
    void validateEmailRejectsEmptyAndNoAt() {
        assertThatThrownBy(() -> UserService.validateEmail("")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> UserService.validateEmail("notanemail")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validatePasswordRejectsShort() {
        assertThatThrownBy(() -> UserService.validatePassword("1234567")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> UserService.validatePassword(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
