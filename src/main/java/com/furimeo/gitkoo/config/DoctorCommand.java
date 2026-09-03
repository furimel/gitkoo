package com.furimeo.gitkoo.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * A diagnostic {@code doctor} command (DESIGN.md §2, §37).
 *
 * <p>Invoked with {@code java -jar gitkoo.jar doctor}. Prints the result of a set of
 * environment checks \u2014 Java version, Git binary, data directory, database, and SSH
 * configuration \u2014 each marked {@code \u2713} (ok) or {@code \u2717} (failed), then exits so the
 * rest of the application does not start.
 *
 * @see DESIGN.md §2 (one-command setup), §37 (configuration)
 */
@Component
public class DoctorCommand implements ApplicationRunner {

    private final GitKooProperties properties;
    private final DataSource dataSource;

    public DoctorCommand(GitKooProperties properties, DataSource dataSource) {
        this.properties = properties;
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (args == null || !isDoctorRequested(args)) {
            return;
        }
        runChecks();
        System.exit(0);
    }

    /**
     * Returns true if {@code doctor} was passed. {@code java -jar gitkoo.jar doctor} passes
     * it as a non-option argument, while {@code --doctor} passes it as an option flag; both
     * forms are accepted.
     */
    private boolean isDoctorRequested(ApplicationArguments args) {
        return args.containsOption("doctor")
                || args.getNonOptionArgs().stream().anyMatch("doctor"::equals);
    }

    private void runChecks() {
        System.out.println("GitKoo doctor");
        System.out.println("============");

        checkJavaVersion();
        checkGitBinary();
        checkDataDirectory();
        checkDatabase();
        checkSsh();

        System.out.println("============");
    }

    private void checkJavaVersion() {
        String version = System.getProperty("java.version");
        String runtime = System.getProperty("java.runtime.name");
        System.out.println(mark(true) + " Java: " + version + " (" + runtime + ")");
    }

    private void checkGitBinary() {
        String binary = properties.getGit().getBinary();
        try {
            Process process = new ProcessBuilder(Arrays.asList(binary, "--version"))
                    .redirectErrorStream(true).start();
            String output = new String(process.getInputStream().readAllBytes()).trim();
            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            boolean ok = finished && process.exitValue() == 0;
            System.out.println(mark(ok) + " Git binary (" + binary + "): "
                    + (ok ? output : "git --version failed"));
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            System.out.println(mark(false) + " Git binary (" + binary + "): not found (" + e.getMessage() + ")");
        }
    }

    private void checkDataDirectory() {
        Path dataPath = Path.of(properties.getData()).toAbsolutePath().normalize();
        boolean exists = Files.isDirectory(dataPath);
        System.out.println(mark(exists) + " Data directory: " + dataPath
                + (exists ? "" : " (missing)"));
    }

    private void checkDatabase() {
        try (var conn = dataSource.getConnection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT 1")) {
            boolean ok = rs.next();
            System.out.println(mark(ok) + " Database: "
                    + (ok ? "SELECT 1 succeeded" : "no result from SELECT 1"));
        } catch (Exception e) {
            System.out.println(mark(false) + " Database: connection failed (" + e.getMessage() + ")");
        }
    }

    private void checkSsh() {
        boolean enabled = properties.getSsh().isEnabled();
        int port = properties.getSsh().getPort();
        System.out.println(mark(true) + " SSH: enabled=" + enabled + ", port=" + port);
    }

    private String mark(boolean ok) {
        return ok ? "\u2713" : "\u2717";
    }
}
