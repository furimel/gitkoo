package com.furimeo.gitkoo.git;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.furimeo.gitkoo.auth.AccessTokenService;
import com.furimeo.gitkoo.auth.UserService;
import com.furimeo.gitkoo.config.GitKooProperties;
import com.furimeo.gitkoo.repository.Repository;
import com.furimeo.gitkoo.repository.RepositoryService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Git smart-HTTP protocol endpoint (DESIGN.md §6, §118).
 *
 * <p>Handles clone/push/pull over HTTPS by invoking {@code git-upload-pack} and
 * {@code git-receive-pack} directly in stateless-RPC mode, rather than wrapping
 * {@code git http-backend} as a CGI. This avoids CGI environment plumbing and is
 * more portable across platforms.
 *
 * <p>Routes (matching both {@code /{owner}/{name}} and {@code /{owner}/{name}.git}):
 * <ul>
 *   <li>{@code GET  info/refs?service=git-upload-pack|git-receive-pack} — advertise refs</li>
 *   <li>{@code POST git-upload-pack} — fetch</li>
 *   <li>{@code POST git-receive-pack} — push</li>
 * </ul>
 *
 * @see DESIGN.md §6, §118
 */
@Controller
public class GitHttpController {

    private static final Logger log = LoggerFactory.getLogger(GitHttpController.class);

    private final GitKooProperties properties;
    private final RepositoryService repositoryService;
    private final UserService userService;
    private final AccessTokenService accessTokenService;
    private final String gitBinary;

    public GitHttpController(GitKooProperties properties, RepositoryService repositoryService,
                            UserService userService, AccessTokenService accessTokenService) {
        this.properties = properties;
        this.repositoryService = repositoryService;
        this.userService = userService;
        this.accessTokenService = accessTokenService;
        this.gitBinary = properties.getGit().getBinary();
    }

    // ── info/refs ────────────────────────────────────────────────────────

    @GetMapping(value = {"/{owner}/{name}/info/refs", "/{owner}/{name}.git/info/refs"})
    public void infoRefs(@PathVariable String owner, @PathVariable String name,
                        HttpServletRequest request, HttpServletResponse response) throws IOException {
        String service = request.getParameter("service");
        if (service == null) {
            // Dumb HTTP — not supported; direct clients to use smart protocol.
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Only smart HTTP is supported");
            return;
        }

        Repository repo = resolveRepo(owner, name);
        if (repo == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // Permission: upload-pack = READ, receive-pack = WRITE.
        boolean needWrite = "git-receive-pack".equals(service);
        if (needWrite && !checkWrite(owner, request)) {
            response.setHeader("WWW-Authenticate", "Basic realm=\"GitKoo\"");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        Path storagePath = Path.of(repo.getStoragePath());

        String contentType = "application/x-" + service + "-advertisement";
        response.setContentType(contentType);
        // No charset — git protocol is binary.

        // Smart protocol: write service announcement line, then git's ref advertisement.
        OutputStream out = response.getOutputStream();
        String announce = "# service=" + service + "\n";
        writePktLine(out, announce);
        out.flush();

        ProcessBuilder pb = new ProcessBuilder(
                gitBinary, service.substring(4), // "upload-pack" or "receive-pack"
                "--stateless-rpc", "--advertise-refs", storagePath.toString());
        pb.redirectErrorStream(false);
        Process process = pb.start();
        transferStream(process.getInputStream(), out);
        process.destroy();
        out.flush();
    }

    // ── upload-pack (fetch/clone) ────────────────────────────────────────

    @PostMapping(value = {"/{owner}/{name}/git-upload-pack", "/{owner}/{name}.git/git-upload-pack"})
    public ResponseEntity<StreamingResponseBody> uploadPack(
            @PathVariable String owner, @PathVariable String name,
            HttpServletRequest request) {

        Repository repo = resolveRepo(owner, name);
        if (repo == null) {
            return ResponseEntity.notFound().build();
        }

        Path storagePath = Path.of(repo.getStoragePath());
        StreamingResponseBody body = out -> runStatelessRpc(
                "git-upload-pack", storagePath, request.getInputStream(), out);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/x-git-upload-pack-result"))
                .body(body);
    }

    // ── receive-pack (push) ──────────────────────────────────────────────

    @PostMapping(value = {"/{owner}/{name}/git-receive-pack", "/{owner}/{name}.git/git-receive-pack"})
    public ResponseEntity<StreamingResponseBody> receivePack(
            @PathVariable String owner, @PathVariable String name,
            HttpServletRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (!checkWrite(owner, request)) {
            return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED)
                    .header("WWW-Authenticate", "Basic realm=\"GitKoo\"")
                    .build();
        }

        Repository repo = resolveRepo(owner, name);
        if (repo == null) {
            return ResponseEntity.notFound().build();
        }

        Path storagePath = Path.of(repo.getStoragePath());
        StreamingResponseBody body = out -> {
            runStatelessRpc("git-receive-pack", storagePath, request.getInputStream(), out);
            // After a successful push, fire post-receive hooks (activity, workflow trigger).
            // This is best-effort — the push already succeeded.
            try {
                runPostReceiveHooks(storagePath, repo);
            } catch (Exception e) {
                log.warn("Post-receive hook failed for repo {}", repo.getId(), e);
            }
        };
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/x-git-receive-pack-result"))
                .body(body);
    }

    // ── helpers ──────────────────────────────────────────────────────────

    /**
     * Runs git-upload-pack or git-receive-pack in stateless-RPC mode, streaming
     * the request body to stdin and stdout to the response.
     */
    private void runStatelessRpc(String service, Path storagePath, InputStream stdin, OutputStream stdout)
            throws IOException {
        ProcessBuilder pb = new ProcessBuilder(
                gitBinary, service.substring(4), "--stateless-rpc", storagePath.toString());
        pb.redirectErrorStream(false);
        Process process = pb.start();

        // Stream request body → process stdin (in a separate thread to avoid deadlock).
        Thread writer = new Thread(() -> {
            OutputStream pIn = process.getOutputStream();
            try (pIn) {
                transferStream(stdin, pIn);
            } catch (IOException e) {
                log.debug("stdin transfer interrupted", e);
            }
        }, "git-stdin");
        writer.setDaemon(true);
        writer.start();

        // Stream process stdout → response.
        InputStream pOut = process.getInputStream();
        try (pOut) {
            transferStream(pOut, stdout);
        }
        stdout.flush();

        try {
            writer.join(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        process.destroy();
    }

    /** Writes a pkt-line: 4-byte hex length + payload. */
    private void writePktLine(OutputStream out, String payload) throws IOException {
        int totalLen = payload.length() + 4;
        String header = String.format("%04x", totalLen);
        out.write(header.getBytes(StandardCharsets.US_ASCII));
        out.write(payload.getBytes(StandardCharsets.UTF_8));
    }

    /** Transfers bytes from in to out until EOF. */
    private void transferStream(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) != -1) {
            out.write(buf, 0, n);
        }
        out.flush();
    }

    /**
     * Runs the post-receive hook script to trigger activity and workflow events.
     * Reads ref updates from the hook and fires them via an internal event.
     */
    private void runPostReceiveHooks(Path storagePath, Repository repo) throws IOException, InterruptedException {
        // For MVP, log the push. Full hook-to-event wiring (activity, workflow trigger)
        // lands when the event system (Phase 7) is ready.
        log.info("Post-receive: push completed for repository {} (id={})",
                repo.getName(), repo.getId());
    }

    private Repository resolveRepo(String owner, String name) {
        // Strip .git suffix if present.
        String cleanName = name.endsWith(".git") ? name.substring(0, name.length() - 4) : name;
        var user = userService.findByUsername(owner);
        if (user.isPresent()) {
            return repositoryService.findByOwnerAndName(
                    Repository.OwnerType.USER.name(), user.get().getId(), cleanName).orElse(null);
        }
        return null;
    }

    /**
     * Checks write permission for git push. MVP: the user must be authenticated
     * (via session or access token) and be the repository owner. Full permission
     * model (team roles, repository_members) lands in Phase 4.
     */
    private boolean checkWrite(String owner, HttpServletRequest request) {
        // Try Bearer token first.
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7).trim();
            var resolved = accessTokenService.resolve(token);
            if (resolved.isPresent()) {
                var user = userService.findById(resolved.get().userId());
                if (user.isPresent()) {
                    return user.get().getUsername().equals(owner);
                }
            }
        }
        // Basic auth (git sends username:password or username:token).
        // For MVP, accept any authenticated session principal as owner.
        var principal = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        if (principal != null && principal.isAuthenticated()
                && !"anonymousUser".equals(principal.getName())) {
            return principal.getName().equals(owner);
        }
        return false;
    }
}
