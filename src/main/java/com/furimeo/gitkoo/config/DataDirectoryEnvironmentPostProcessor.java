package com.furimeo.gitkoo.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Creates the data root directory very early, before the DataSource auto-configuration
 * tries to open the SQLite database file. SQLite needs the parent directory to exist.
 *
 * <p>The subdirectories under the data root are created later by {@link StorageInitializer}
 * once the application context is up.
 *
 * @see DESIGN.md §2 (zero-config)
 */
public class DataDirectoryEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(DataDirectoryEnvironmentPostProcessor.class);

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String data = environment.getProperty("gitkoo.data", "./data");
        Path root = Paths.get(data).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
            log.info("Data root ensured at {}", root);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create data directory at " + root, e);
        }
    }
}
