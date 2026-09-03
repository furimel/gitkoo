package com.furimeo.gitkoo.git;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.security.PublicKey;
import java.util.Optional;

import org.apache.sshd.common.config.keys.KeyUtils;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.pubkey.PublickeyAuthenticator;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.AbstractCommandSupport;
import org.apache.sshd.server.command.Command;
import org.apache.sshd.server.command.CommandFactory;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Component;

import com.furimeo.gitkoo.auth.SshKey;
import com.furimeo.gitkoo.auth.SshKeyRepository;
import com.furimeo.gitkoo.auth.User;
import com.furimeo.gitkoo.auth.UserRepository;
import com.furimeo.gitkoo.config.GitKooProperties;
import com.furimeo.gitkoo.repository.Repository;
import com.furimeo.gitkoo.repository.RepositoryService;

import jakarta.annotation.PreDestroy;

/**
 * SSH server for Git access over SSH (DESIGN.md §8).
 *
 * <p>Authenticates via registered SSH public keys (looked up from the database by
 * fingerprint), then dispatches {@code git-upload-pack} and {@code git-receive-pack}
 * to the resolved repository's bare Git path.
 *
 * @see DESIGN.md §8, §6
 */
@Component
public class GitSshServer implements SmartInitializingSingleton {

    private static final Logger log = LoggerFactory.getLogger(GitSshServer.class);

    private final GitKooProperties properties;
    private final SshKeyRepository sshKeyRepository;
    private final UserRepository userRepository;
    private final RepositoryService repositoryService;

    private SshServer sshd;

    public GitSshServer(GitKooProperties properties, SshKeyRepository sshKeyRepository,
                       UserRepository userRepository, RepositoryService repositoryService) {
        this.properties = properties;
        this.sshKeyRepository = sshKeyRepository;
        this.userRepository = userRepository;
        this.repositoryService = repositoryService;
    }

    @Override
    public void afterSingletonsInstantiated() {
        if (!properties.getSsh().isEnabled()) {
            log.info("SSH server disabled by configuration");
            return;
        }

        Path hostKeyPath = Path.of(properties.getData()).toAbsolutePath().normalize()
                .resolve(".ssh_host_key");

        sshd = SshServer.setUpDefaultServer();
        sshd.setPort(properties.getSsh().getPort());
        sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(hostKeyPath));

        sshd.setPublickeyAuthenticator(new GitPublickeyAuthenticator(sshKeyRepository, userRepository));

        String gitBinary = properties.getGit().getBinary();
        sshd.setCommandFactory(new GitCommandFactory(gitBinary, repositoryService));

        try {
            sshd.start();
            log.info("GitKoo SSH server started on port {}", properties.getSsh().getPort());
        } catch (IOException e) {
            log.error("Failed to start SSH server on port {}", properties.getSsh().getPort(), e);
        }
    }

    @PreDestroy
    public void stop() {
        if (sshd != null) {
            try {
                sshd.stop(true);
                log.info("GitKoo SSH server stopped");
            } catch (IOException e) {
                log.warn("Error stopping SSH server", e);
            }
        }
    }

    // ── public key authentication ──────────────────────────────────────────

    static class GitPublickeyAuthenticator implements PublickeyAuthenticator {

        private static final Logger log = LoggerFactory.getLogger(GitPublickeyAuthenticator.class);

        private final SshKeyRepository sshKeyRepository;
        private final UserRepository userRepository;

        GitPublickeyAuthenticator(SshKeyRepository sshKeyRepository, UserRepository userRepository) {
            this.sshKeyRepository = sshKeyRepository;
            this.userRepository = userRepository;
        }

        @Override
        public boolean authenticate(String username, PublicKey key, ServerSession session) throws org.apache.sshd.server.auth.AsyncAuthException {
            String fingerprint = KeyUtils.getFingerPrint(key);
            if (fingerprint == null) {
                return false;
            }
            Optional<SshKey> stored = sshKeyRepository.findByFingerprint(fingerprint);
            if (stored.isEmpty()) {
                log.debug("SSH key fingerprint {} not found", fingerprint);
                return false;
            }
            Optional<User> user = userRepository.findById(stored.get().getUserId());
            if (user.isEmpty() || !"ACTIVE".equals(user.get().getStatus())) {
                return false;
            }
            if (!user.get().getUsername().equalsIgnoreCase(username)) {
                log.debug("SSH username '{}' does not match key owner '{}'", username, user.get().getUsername());
                return false;
            }
            return true;
        }
    }

    // ── command factory ────────────────────────────────────────────────────

    static class GitCommandFactory implements CommandFactory {

        private final String gitBinary;
        private final RepositoryService repositoryService;

        GitCommandFactory(String gitBinary, RepositoryService repositoryService) {
            this.gitBinary = gitBinary;
            this.repositoryService = repositoryService;
        }

        @Override
        public Command createCommand(ChannelSession channel, String command) throws IOException {
            return new GitCommand(gitBinary, repositoryService, command);
        }
    }

    // ── git command execution ──────────────────────────────────────────────

    static class GitCommand extends AbstractCommandSupport {

        private static final Logger log = LoggerFactory.getLogger(GitCommand.class);

        private final String gitBinary;
        private final RepositoryService repositoryService;

        GitCommand(String gitBinary, RepositoryService repositoryService, String command) {
            super(command, null);
            this.gitBinary = gitBinary;
            this.repositoryService = repositoryService;
        }

        @Override
        public void run() {
            String[] parsed = parseGitCommand(getCommand());
            if (parsed == null) {
                onExit(1);
                return;
            }
            String gitService = parsed[0];
            String ownerSlashName = parsed[1];

            Repository repo = resolveRepo(ownerSlashName);
            if (repo == null) {
                try {
                    getOutputStream().write(("Repository not found: " + ownerSlashName + "\n").getBytes());
                } catch (IOException ignored) {}
                onExit(1);
                return;
            }

            try {
                Process process = new ProcessBuilder(gitBinary, gitService, repo.getStoragePath()).start();
                Thread t1 = pipe(getInputStream(), process.getOutputStream());
                Thread t2 = pipe(process.getInputStream(), getOutputStream());
                Thread t3 = pipe(process.getErrorStream(), getOutputStream());

                int code = process.waitFor();
                t1.interrupt();
                t2.interrupt();
                t3.interrupt();
                onExit(code);
            } catch (Exception e) {
                log.error("SSH git command failed", e);
                onExit(1);
            }
        }

        private Repository resolveRepo(String ownerSlashName) {
            String[] parts = ownerSlashName.split("/", 2);
            if (parts.length != 2) return null;
            return repositoryService.findByOwnerUsernameAndName(parts[0], parts[1]).orElse(null);
        }

        /** Parses "git-upload-pack 'owner/repo.git'" into [service, "owner/repo"]. */
        static String[] parseGitCommand(String cmd) {
            if (cmd == null) return null;
            int space = cmd.indexOf(' ');
            if (space < 0) return null;
            String service = cmd.substring(0, space);
            if (!"git-upload-pack".equals(service) && !"git-receive-pack".equals(service)) {
                return null;
            }
            String path = cmd.substring(space + 1).trim();
            if (path.startsWith("'") && path.endsWith("'")) {
                path = path.substring(1, path.length() - 1);
            }
            if (path.endsWith(".git")) {
                path = path.substring(0, path.length() - 4);
            }
            if (!path.contains("/")) return null;
            return new String[]{service, path};
        }

        private static Thread pipe(InputStream in, OutputStream out) {
            Thread t = new Thread(() -> {
                byte[] buf = new byte[8192];
                int n;
                try {
                    while (!Thread.currentThread().isInterrupted() && (n = in.read(buf)) != -1) {
                        out.write(buf, 0, n);
                        out.flush();
                    }
                } catch (IOException ignored) {}
            }, "ssh-pipe");
            t.setDaemon(true);
            t.start();
            return t;
        }
    }
}
