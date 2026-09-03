package com.furimeo.gitkoo.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.furimeo.gitkoo.config.GitKooProperties;

/**
 * Ensures the on-disk storage layout exists on startup.
 *
 * <p>Creates {@code data/{git,artifacts,attachments,logs}} under the configured data root.
 * Runs before application-ready so other components can assume the directories exist.
 *
 * @see DESIGN.md §2 (zero-config), §5 (storage layout)
 */
@Component
@Order(0)
public class StorageInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(StorageInitializer.class);

    private static final List<String> SUBDIRS = List.of("git/repositories", "artifacts", "attachments", "logs");

    private final GitKooProperties properties;

    public StorageInitializer(GitKooProperties properties) {
        this.properties = properties;
    }

    @Override
    public void run(String... args) {
        Path root = Path.of(properties.getData()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
            for (String sub : SUBDIRS) {
                Files.createDirectories(root.resolve(sub));
            }
            log.info("Data directory initialized at {}", root);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialize data directory at " + root, e);
        }
    }
}
