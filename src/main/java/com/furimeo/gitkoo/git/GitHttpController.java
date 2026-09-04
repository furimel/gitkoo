package com.furimeo.gitkoo.git;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import com.furimeo.gitkoo.activity.ActivityService;
import com.furimeo.gitkoo.auth.AccessTokenService;
import com.furimeo.gitkoo.auth.User;
import com.furimeo.gitkoo.auth.UserService;
import com.furimeo.gitkoo.config.GitKooProperties;
import com.furimeo.gitkoo.repository.ProtectedBranchService;
import com.furimeo.gitkoo.repository.Repository;
import com.furimeo.gitkoo.repository.RepositoryPermissionService;
import com.furimeo.gitkoo.repository.RepositoryPermissionService.Permission;
import com.furimeo.gitkoo.repository.RepositoryService;
import com.furimeo.gitkoo.workflow.WorkflowService;
import com.furimeo.gitkoo.workflow.ast.Workflow;

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
 *   <li>{@code GET  info/refs?service=git-upload-pack|git-receive-pack} - advertise refs</li>
 *   <li>{@code POST git-upload-pack} - fetch</li>
 *   <li>{@code POST git-receive-pack} - push</li>
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
    private final ActivityService activityService;
    private final WorkflowService workflowService;
    private final GitService gitService;
    private final RepositoryPermissionService permissionService;
    private final ProtectedBranchService protectedBranchService;
    private final String gitBinary;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    public GitHttpController(GitKooProperties properties, RepositoryService repositoryService,
                            UserService userService, AccessTokenService accessTokenService,
                            ActivityService activityService, WorkflowService workflowService,
                            GitService gitService, RepositoryPermissionService permissionService,
                            ProtectedBranchService protectedBranchService,
                            org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
        this.repositoryService = repositoryService;
        this.userService = userService;
        this.accessTokenService = accessTokenService;
        this.activityService = activityService;
        this.workflowService = workflowService;
        this.gitService = gitService;
        this.permissionService = permissionService;
        this.protectedBranchService = protectedBranchService;
        this.gitBinary = properties.getGit().getBinary();
    }

    // ── info/refs ────────────────────────────────────────────────────────

    @GetMapping(value = {"/{owner}/{name}/info/refs", "/{owner}/{name}.git/info/refs"})
    public void infoRefs(@PathVariable String owner, @PathVariable String name,
                        HttpServletRequest request, HttpServletResponse response) throws IOException {
        String service = request.getParameter("service");
        if (service == null) {
            // Dumb HTTP - not supported; direct clients to use smart protocol.
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
        User actor = resolveActor(request);
        if (needWrite) {
            if (actor == null || !permissionService.hasPermission(actor, repo, Permission.WRITE)) {
                response.setHeader("WWW-Authenticate", "Basic realm=\"GitKoo\"");
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        } else {
            // upload-pack = READ. PUBLIC repos grant READ to anonymous; PRIVATE repos
            // require an explicit grant (NONE permission is rejected).
            if (!permissionService.hasPermission(actor, repo, Permission.READ)) {
                response.setHeader("WWW-Authenticate", "Basic realm=\"GitKoo\"");
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }

        Path storagePath = Path.of(repo.getStoragePath());

        String contentType = "application/x-" + service + "-advertisement";
        response.setContentType(contentType);
        // No charset - git protocol is binary.

        /*
         * Smart protocol ref advertisement, per Documentation/http-protocol.txt:
         *
         *   PKT-LINE("# service=$servicename" LF)
         *   "0000"
         *   ref_list
         *   "0000"
         *
         * The "0000" flush packet after the service line is mandatory. Without it a
         * client reads the first ref as a continuation of the header and gives up with
         * "Could not read from remote repository", which is why cloning over HTTP had
         * never worked. git itself emits the trailing flush with --advertise-refs.
         */
        OutputStream out = response.getOutputStream();
        writePktLine(out, "# service=" + service + "\n");
        out.write(FLUSH_PKT);
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

        User actor = resolveActor(request);
        if (!permissionService.hasPermission(actor, repo, Permission.READ)) {
            return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED)
                    .header("WWW-Authenticate", "Basic realm=\"GitKoo\"")
                    .build();
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

        Repository repo = resolveRepo(owner, name);
        if (repo == null) {
            return ResponseEntity.notFound().build();
        }

        User actor = resolveActor(request);
        if (actor == null || !permissionService.hasPermission(actor, repo, Permission.WRITE)) {
            return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED)
                    .header("WWW-Authenticate", "Basic realm=\"GitKoo\"")
                    .build();
        }

        Path storagePath = Path.of(repo.getStoragePath());
        StreamingResponseBody body = out -> {
            // Snapshot branch heads before the push so we can compute what changed.
            Map<String, String> before = branchHeads(storagePath);
            runStatelessRpc("git-receive-pack", storagePath, request.getInputStream(), out);
            // After a successful push, fire post-receive hooks (activity, workflow trigger).
            // This is best-effort - the push already succeeded.
            try {
                Map<String, String> after = branchHeads(storagePath);
                List<RefUpdate> pushed = pushedRefs(before, after);
                runPostReceiveHooks(storagePath, repo, pushed);
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

    /**
     * Resolves HTTP Basic credentials to a user.
     *
     * <p>The password may be the account password or an access token, because that is
     * how people actually use Git over HTTP: credential helpers store one string, and
     * a token is the safer thing to store.
     *
     * @param encoded the base64 payload of the Authorization header
     * @return the authenticated user, or null when the credentials do not check out
     */
    private User resolveBasic(String encoded) {
        String decoded;
        try {
            decoded = new String(java.util.Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return null;
        }
        int colon = decoded.indexOf(':');
        if (colon < 0) {
            return null;
        }
        String username = decoded.substring(0, colon);
        String secret = decoded.substring(colon + 1);

        // A token authenticates on its own; the username beside it is ignored, which
        // is what lets `git clone https://token@host/...` work.
        var byToken = accessTokenService.resolve(secret);
        if (byToken.isPresent()) {
            return userService.findById(byToken.get().userId()).orElse(null);
        }

        User user = userService.findByUsername(username).orElse(null);
        if (user == null || user.getPasswordHash() == null) {
            return null;
        }
        return passwordEncoder.matches(secret, user.getPasswordHash()) ? user : null;
    }

    /** The pkt-line flush packet, which ends a section of the Git wire protocol. */
    private static final byte[] FLUSH_PKT = "0000".getBytes(StandardCharsets.US_ASCII);

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
     * Runs the post-receive hooks to record activity and trigger workflows for the
     * refs that changed during the push. Best-effort: the push already succeeded.
     */
    private void runPostReceiveHooks(Path storagePath, Repository repo, List<RefUpdate> pushedRefs) {
        activityService.record(repo.getId(), null, "GIT_PUSHED", "pushed to " + repo.getName());
        for (RefUpdate ref : pushedRefs) {
            try {
                triggerWorkflows(storagePath, repo, ref.branch(), ref.commitSha());
            } catch (Exception e) {
                log.warn("Workflow trigger failed for repo {} branch {}", repo.getId(), ref.branch(), e);
            }
        }
    }

    /**
     * Finds {@code .gitkoo/workflows/*.koo} files on the pushed branch, parses each,
     * and triggers the workflows whose {@code on push [branch]} trigger matches the
     * pushed branch (DESIGN.md §31).
     */
    private void triggerWorkflows(Path storagePath, Repository repo, String branch, String commitSha) {
        List<GitService.TreeEntry> entries = gitService.listTree(storagePath, branch, ".gitkoo/workflows");
        for (GitService.TreeEntry entry : entries) {
            if (!entry.isBlob() || !entry.name().endsWith(".koo")) {
                continue;
            }
            try {
                String source = gitService.catFile(storagePath, branch,
                        ".gitkoo/workflows/" + entry.name());
                if (source == null || source.isBlank()) {
                    continue;
                }
                Workflow workflow = workflowService.parse(source);
                for (Workflow.Trigger trigger : workflow.triggers()) {
                    if (!"push".equals(trigger.event())) {
                        continue;
                    }
                    // filter is an optional branch name; null means "any branch".
                    if (trigger.filter() != null && !trigger.filter().equals(branch)) {
                        continue;
                    }
                    workflowService.trigger(workflow, repo, "push", "refs/heads/" + branch,
                            commitSha, null);
                }
            } catch (Exception e) {
                log.warn("Failed to process workflow file {} in repo {}", entry.name(), repo.getId(), e);
            }
        }
    }

    /** Returns branch name → commit SHA for all local branches of the repository. */
    private Map<String, String> branchHeads(Path storagePath) {
        Map<String, String> heads = new HashMap<>();
        GitService.GitResult result = gitService.run(storagePath, "for-each-ref",
                "--format=%(refname:short) %(objectname)", "refs/heads/");
        if (!result.success()) {
            return heads;
        }
        for (String line : result.stdout().lines().toList()) {
            if (line.isBlank()) {
                continue;
            }
            String[] parts = line.split("\\s+", 2);
            if (parts.length == 2) {
                heads.put(parts[0], parts[1]);
            }
        }
        return heads;
    }

    /** Computes the branch refs that are new or updated between two snapshots. */
    private List<RefUpdate> pushedRefs(Map<String, String> before, Map<String, String> after) {
        List<RefUpdate> pushed = new ArrayList<>();
        for (Map.Entry<String, String> e : after.entrySet()) {
            String oldSha = before.get(e.getKey());
            if (oldSha == null || !oldSha.equals(e.getValue())) {
                pushed.add(new RefUpdate(e.getKey(), e.getValue()));
            }
        }
        return pushed;
    }

    /** A branch ref updated by a push: branch name + new commit SHA. */
    private record RefUpdate(String branch, String commitSha) {}

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
     * Resolves the authenticated user for a git transport request. Tries a Bearer
     * personal access token first (git clients send it as the password in Basic
     * auth, or directly as a Bearer header), then falls back to the session
     * principal populated by the {@link AccessTokenAuthenticationFilter}.
     *
     * @return the acting user, or {@code null} if the request is anonymous
     */
    private User resolveActor(HttpServletRequest request) {
        // Try Bearer token first.
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7).trim();
            var resolved = accessTokenService.resolve(token);
            if (resolved.isPresent()) {
                return userService.findById(resolved.get().userId()).orElse(null);
            }
        }
        /*
         * HTTP Basic, which is what a Git client actually sends. This branch was
         * missing: the 401 above challenges with WWW-Authenticate: Basic and the
         * credentials that came back were then ignored, so the client retried, got
         * challenged again and gave up with "Authentication failed". Cloning or
         * pushing a private repository over HTTP could never succeed.
         */
        if (auth != null && auth.startsWith("Basic ")) {
            User actor = resolveBasic(auth.substring(6).trim());
            if (actor != null) {
                return actor;
            }
        }
        // Fall back to the security context (session principal or token filter).
        var principal = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        if (principal != null && principal.isAuthenticated()
                && !"anonymousUser".equals(principal.getName())) {
            return userService.findByUsername(principal.getName()).orElse(null);
        }
        return null;
    }
}
