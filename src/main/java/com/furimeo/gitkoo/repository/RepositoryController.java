package com.furimeo.gitkoo.repository;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.furimeo.gitkoo.auth.User;
import com.furimeo.gitkoo.auth.UserService;
import com.furimeo.gitkoo.git.GitService;
import com.furimeo.gitkoo.git.GitService.CommitInfo;
import com.furimeo.gitkoo.git.GitService.TreeEntry;
import com.furimeo.gitkoo.repository.RepositoryPermissionService;
import com.furimeo.gitkoo.repository.RepositoryPermissionService.Permission;

/**
 * Repository creation and code browsing (DESIGN.md §12, §69).
 *
 * <p>MVP supports USER-owned repositories only; team ownership lands in a later phase.
 * Routes use {@code /{username}/{name}} so URLs are human-friendly while the on-disk
 * storage stays ID-based (DESIGN.md §70).
 */
@Controller
@RequestMapping
public class RepositoryController {

    private final RepositoryService repositoryService;
    private final RepositoryRepository repositoryRepository;
    private final UserService userService;
    private final GitService gitService;
    private final RepositoryPermissionService permissionService;
    private final com.furimeo.gitkoo.web.MarkdownService markdownService;
    private final com.furimeo.gitkoo.config.GitKooProperties properties;
    // Injected here rather than taken as a handler argument: Spring MVC treats an
    // unannotated method parameter as a command object and builds a blank instance,
    // which left every field null and 500'd the activity page.
    private final com.furimeo.gitkoo.activity.ActivityService activityService;

    public RepositoryController(RepositoryService repositoryService, RepositoryRepository repositoryRepository,
                                UserService userService, GitService gitService,
                                RepositoryPermissionService permissionService,
                                com.furimeo.gitkoo.web.MarkdownService markdownService,
                                com.furimeo.gitkoo.config.GitKooProperties properties,
                                com.furimeo.gitkoo.activity.ActivityService activityService) {
        this.repositoryService = repositoryService;
        this.repositoryRepository = repositoryRepository;
        this.userService = userService;
        this.gitService = gitService;
        this.permissionService = permissionService;
        this.markdownService = markdownService;
        this.properties = properties;
        this.activityService = activityService;
    }

    // ── create ──────────────────────────────────────────────────────────

    @GetMapping("/new")
    public String newRepositoryForm(Model model) {
        model.addAttribute("title", "New repository");
        return "repository/new";
    }

    @PostMapping("/new")
    public String createRepository(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam(defaultValue = "PUBLIC") String visibility,
            @RequestParam(defaultValue = "main") String defaultBranch,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
            RedirectAttributes redirectAttributes) {

        User owner = userService.findByUsername(principal.getUsername())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
        try {
            Repository repo = repositoryService.create(
                    Repository.OwnerType.USER.name(), owner.getId(),
                    name, description, visibility, defaultBranch);
            return "redirect:/" + owner.getUsername() + "/" + repo.getName();
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("name", name);
            redirectAttributes.addFlashAttribute("description", description);
            return "redirect:/new";
        }
    }

    // ── browse ──────────────────────────────────────────────────────────

    @GetMapping("/{username}/{name}")
    public String repositoryOverview(@PathVariable String username, @PathVariable String name,
                                     Model model,
                                     @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        Repository repo = resolve(username, name);
        requireRead(principal, repo);
        return browse(model, username, repo, repo.getDefaultBranch(), "");
    }

    /** Browsing a directory inside the tree: {@code /{user}/{repo}/tree/{ref}/path/to/dir}. */
    @GetMapping({"/{username}/{name}/tree/{ref}", "/{username}/{name}/tree/{ref}/**"})
    public String treeBrowser(@PathVariable String username, @PathVariable String name,
                              @PathVariable String ref,
                              @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
                              jakarta.servlet.http.HttpServletRequest request,
                              Model model) {
        Repository repo = resolve(username, name);
        requireRead(principal, repo);
        String path = pathAfter(request, "/" + username + "/" + name + "/tree/" + ref);
        return browse(model, username, repo, ref, path);
    }

    /**
     * Renders the code browser for one ref and directory.
     *
     * <p>Supplies everything the page shows at a glance (DESIGN.md \u00a756): branch list,
     * commit count, the latest commit, the directory listing with directories first,
     * clone URLs, and the rendered README.
     */
    private String browse(Model model, String username, Repository repo, String ref, String path) {
        addRepoHeader(model, username, repo);
        Path storagePath = Path.of(repo.getStoragePath());

        model.addAttribute("cloneUrl", cloneUrl(username, repo.getName()));
        model.addAttribute("sshCloneUrl", sshCloneUrl(username, repo.getName()));

        if (gitService.resolveRef(storagePath, "refs/heads/" + ref).isBlank()) {
            model.addAttribute("empty", true);
            model.addAttribute("branches", List.of());
            return "repository/code";
        }

        List<TreeEntry> entries = new java.util.ArrayList<>(gitService.listTree(storagePath, ref, path));
        // Directories first, then files, each alphabetically - the order Git users expect.
        entries.sort(java.util.Comparator
                .comparing(TreeEntry::isDirectory).reversed()
                .thenComparing(e -> e.name().toLowerCase(java.util.Locale.ROOT)));

        List<CommitInfo> commits = gitService.log(storagePath, ref, 1);

        model.addAttribute("ref", ref);
        model.addAttribute("path", path);
        model.addAttribute("parentPath", parentOf(path));
        model.addAttribute("entries", entries);
        model.addAttribute("branches", gitService.branches(storagePath));
        model.addAttribute("totalCommits", gitService.commitCount(storagePath, ref));
        model.addAttribute("latestCommit", commits.isEmpty() ? null : commits.get(0));

        addReadme(model, storagePath, ref, path, entries);
        return "repository/code";
    }

    /** Renders the first README-like file in the current directory, if there is one. */
    private void addReadme(Model model, Path storagePath, String ref, String path, List<TreeEntry> entries) {
        TreeEntry readme = entries.stream()
                .filter(TreeEntry::isBlob)
                .filter(e -> e.name().toLowerCase(java.util.Locale.ROOT).startsWith("readme"))
                .findFirst()
                .orElse(null);
        if (readme == null) {
            return;
        }
        String full = path.isEmpty() ? readme.name() : path + "/" + readme.name();
        String content = gitService.catFile(storagePath, ref, full);
        if (content.isBlank()) {
            return;
        }
        model.addAttribute("readmeName", readme.name());
        model.addAttribute("readmeHtml", isMarkdown(readme.name())
                ? markdownService.render(content)
                : "<pre>" + org.springframework.web.util.HtmlUtils.htmlEscape(content) + "</pre>");
    }

    /** Everything after {@code prefix} in the request URI, with no leading or trailing slash. */
    private String pathAfter(jakarta.servlet.http.HttpServletRequest request, String prefix) {
        String uri = java.net.URLDecoder.decode(request.getRequestURI(), java.nio.charset.StandardCharsets.UTF_8);
        String rest = uri.startsWith(prefix) ? uri.substring(prefix.length()) : "";
        while (rest.startsWith("/")) {
            rest = rest.substring(1);
        }
        while (rest.endsWith("/")) {
            rest = rest.substring(0, rest.length() - 1);
        }
        return rest;
    }

    /** Parent of a slash-separated path; empty string at the root. */
    private String parentOf(String path) {
        int slash = path.lastIndexOf('/');
        return slash < 0 ? "" : path.substring(0, slash);
    }

    private boolean isMarkdown(String fileName) {
        String lower = fileName.toLowerCase(java.util.Locale.ROOT);
        return lower.endsWith(".md") || lower.endsWith(".markdown");
    }

    private String cloneUrl(String username, String repoName) {
        return baseUrl() + "/" + username + "/" + repoName + ".git";
    }

    private String sshCloneUrl(String username, String repoName) {
        if (!properties.getSsh().isEnabled()) {
            return null;
        }
        int port = properties.getSsh().getPort();
        String host = hostOnly();
        return port == 22
                ? "git@" + host + ":" + username + "/" + repoName + ".git"
                : "ssh://git@" + host + ":" + port + "/" + username + "/" + repoName + ".git";
    }

    /** The URL this request came in on, so the clone box shows something that actually works. */
    private String baseUrl() {
        return org.springframework.web.servlet.support.ServletUriComponentsBuilder
                .fromCurrentContextPath().build().toUriString();
    }

    private String hostOnly() {
        return org.springframework.web.servlet.support.ServletUriComponentsBuilder
                .fromCurrentContextPath().build().getHost();
    }

    @GetMapping("/{username}/{name}/activity")
    public String repositoryActivity(@PathVariable String username, @PathVariable String name,
                                     Model model,
                                     @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        Repository repo = resolve(username, name);
        requireRead(principal, repo);
        model.addAttribute("title", "Activity \u00b7 " + username + "/" + name);
        model.addAttribute("owner", username);
        model.addAttribute("repo", repo);
        model.addAttribute("activities", activityService.listByRepository(repo.getId()));
        return "repository/activity";
    }

    @GetMapping("/{username}/{name}/blob/{ref}/**")
    public String fileViewer(@PathVariable String username, @PathVariable String name,
                            @PathVariable String ref,
                            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
                            org.springframework.web.servlet.mvc.support.RedirectAttributes ra,
                            jakarta.servlet.http.HttpServletRequest request,
                            Model model) {
        Repository repo = resolve(username, name);
        requireRead(principal, repo);
        addRepoHeader(model, username, repo);

        String filePath = pathAfter(request, "/" + username + "/" + name + "/blob/" + ref);
        if (filePath.isEmpty()) {
            return "redirect:/" + username + "/" + name;
        }

        Path storagePath = Path.of(repo.getStoragePath());

        // A directory reached through /blob/ is a mistyped URL, not a file - send it to the tree.
        boolean isDirectory = gitService.listTree(storagePath, ref, filePath).stream().findAny().isPresent()
                && gitService.catFile(storagePath, ref, filePath).isBlank();
        if (isDirectory) {
            return "redirect:/" + username + "/" + name + "/tree/" + ref + "/" + filePath;
        }

        String content = gitService.catFile(storagePath, ref, filePath);
        model.addAttribute("ref", ref);
        model.addAttribute("filePath", filePath);
        model.addAttribute("fileName", filePath.substring(filePath.lastIndexOf('/') + 1));
        model.addAttribute("content", content);
        // One entry per line so the view can lay out a gutter of line numbers.
        model.addAttribute("lines", content.isEmpty() ? List.of() : List.of(content.split("\n", -1)));
        model.addAttribute("byteSize", content.getBytes(java.nio.charset.StandardCharsets.UTF_8).length);
        model.addAttribute("branches", gitService.branches(storagePath));
        model.addAttribute("breadcrumbs", breadcrumbs(filePath));
        if (isMarkdown(filePath)) {
            model.addAttribute("renderedHtml", markdownService.render(content));
        }
        return "repository/file";
    }

    /** Serves a blob verbatim, so "Raw" and direct links to a file work. */
    @GetMapping("/{username}/{name}/raw/{ref}/**")
    @org.springframework.web.bind.annotation.ResponseBody
    public org.springframework.http.ResponseEntity<String> rawFile(
            @PathVariable String username, @PathVariable String name, @PathVariable String ref,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
            jakarta.servlet.http.HttpServletRequest request) {
        Repository repo = resolve(username, name);
        requireRead(principal, repo);
        String filePath = pathAfter(request, "/" + username + "/" + name + "/raw/" + ref);
        if (filePath.isEmpty()) {
            return org.springframework.http.ResponseEntity.notFound().build();
        }
        String content = gitService.catFile(Path.of(repo.getStoragePath()), ref, filePath);
        return org.springframework.http.ResponseEntity.ok()
                // text/plain so the browser shows it instead of executing it.
                .contentType(org.springframework.http.MediaType.TEXT_PLAIN)
                .header("X-Content-Type-Options", "nosniff")
                .body(content);
    }

    /** One clickable crumb per path segment, each carrying the path prefix up to itself. */
    private List<Crumb> breadcrumbs(String filePath) {
        List<Crumb> crumbs = new java.util.ArrayList<>();
        StringBuilder walked = new StringBuilder();
        String[] parts = filePath.split("/");
        for (int i = 0; i < parts.length; i++) {
            if (walked.length() > 0) {
                walked.append('/');
            }
            walked.append(parts[i]);
            crumbs.add(new Crumb(parts[i], walked.toString(), i == parts.length - 1));
        }
        return crumbs;
    }

    /** A single breadcrumb segment: its label, the path it points at, and whether it is the file itself. */
    public record Crumb(String name, String path, boolean last) {
    }

    // ── settings ────────────────────────────────────────────────────────

    @GetMapping("/{username}/{name}/settings")
    public String repositorySettings(@PathVariable String username, @PathVariable String name,
                                     Model model,
                                     @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        Repository repo = resolve(username, name);
        requireWrite(principal, repo);
        addRepoHeader(model, username, repo);
        return "repository/settings";
    }

    @PostMapping("/{username}/{name}/settings")
    public String updateRepositorySettings(@PathVariable String username, @PathVariable String name,
                                           @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
                                           @RequestParam(required = false) String description,
                                           @RequestParam String visibility,
                                           @RequestParam String defaultBranch,
                                           RedirectAttributes redirectAttributes) {
        Repository repo = resolve(username, name);
        requireWrite(principal, repo);
        // Name is read-only and intentionally not updated (storage path is id-based,
        // but renaming is out of scope for the MVP settings page).
        repo.setDescription(description);
        repo.setVisibility(visibility);
        repo.setDefaultBranch(defaultBranch);
        repo.setUpdatedAt(OffsetDateTime.now());
        repositoryRepository.save(repo);
        redirectAttributes.addFlashAttribute("success", "Settings saved.");
        return "redirect:/" + username + "/" + name + "/settings";
    }

    private Repository resolve(String username, String name) {
        User owner = userService.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        return repositoryService.findByOwnerAndName(Repository.OwnerType.USER.name(), owner.getId(), name)
                .orElseThrow(() -> new IllegalArgumentException("Repository not found: " + username + "/" + name));
    }

    /** Requires at least READ permission for the authenticated user, else 403. */
    private void requireRead(org.springframework.security.core.userdetails.User principal, Repository repo) {
        User actor = actor(principal);
        if (!permissionService.hasPermission(actor, repo, Permission.READ)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You do not have read access to " + repo.getName());
        }
    }

    /** Requires at least WRITE permission for the authenticated user, else 403. */
    private void requireWrite(org.springframework.security.core.userdetails.User principal, Repository repo) {
        User actor = actor(principal);
        if (!permissionService.hasPermission(actor, repo, Permission.WRITE)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You do not have write access to " + repo.getName());
        }
    }

    /** Resolves the authenticated user, or null when anonymous. */
    private User actor(org.springframework.security.core.userdetails.User principal) {
        if (principal == null || "anonymousUser".equals(principal.getUsername())) {
            return null;
        }
        return userService.findByUsername(principal.getUsername()).orElse(null);
    }

    private void addRepoHeader(Model model, String username, Repository repo) {
        model.addAttribute("title", username + "/" + repo.getName());
        model.addAttribute("owner", username);
        model.addAttribute("repo", repo);
    }
}
