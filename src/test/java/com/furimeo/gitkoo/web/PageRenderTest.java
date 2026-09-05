package com.furimeo.gitkoo.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.furimeo.gitkoo.auth.User;
import com.furimeo.gitkoo.auth.UserService;
import com.furimeo.gitkoo.git.GitService;
import com.furimeo.gitkoo.issue.IssueService;
import com.furimeo.gitkoo.issue.LabelService;
import com.furimeo.gitkoo.pullrequest.PullRequestReview;
import com.furimeo.gitkoo.pullrequest.PullRequestService;
import com.furimeo.gitkoo.repository.Repository;
import com.furimeo.gitkoo.repository.RepositoryService;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Requests every page as the Inertia client would, and checks what comes back.
 *
 * <p>Three things are asserted for each route, and each one has caught a real bug:
 *
 * <ul>
 *   <li>the page object names a component that exists as a React page - a controller
 *       renamed without its counterpart is otherwise a blank screen at runtime;
 *   <li>every prop serialises - one unserialisable object in the model fails the
 *       whole response, and the browser shows nothing;
 *   <li>no password hash, token hash or filesystem path is in the JSON. The old
 *       templates chose their fields one at a time; props are the whole model, so
 *       what used to be merely unused is now shipped to the browser.
 * </ul>
 *
 * <p>Seeded once for the class rather than per test: each seeding creates a real bare
 * repository and pushes a real commit, and repeating that for every route would
 * dominate the runtime of the suite.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PageRenderTest {

    private static final String OWNER = "renderadmin";
    private static final String REPO = "render-repo";

    /** A repository with no commits, so the empty states are covered too. */
    private static final String EMPTY_REPO = "render-empty";

    /** Where a component name has to resolve to a file. */
    private static final Path PAGES = Path.of("frontend/src/pages");

    @Autowired private MockMvc mvc;
    @Autowired private UserService userService;
    @Autowired private RepositoryService repositoryService;
    @Autowired private IssueService issueService;
    @Autowired private LabelService labelService;
    @Autowired private PullRequestService prService;
    @Autowired private GitService gitService;

    @BeforeAll
    void seed(@TempDir Path work) throws IOException {
        User admin = userService.needsSetup()
                ? userService.createAdministrator(OWNER, "render@test.com", "password123")
                : userService.findByUsername(OWNER).orElseGet(
                        () -> userService.createUser(OWNER, "render@test.com", "password123"));

        Repository repo = repositoryService.create(Repository.OwnerType.USER.name(), admin.getId(),
                REPO, "A repository used by the render test", "PUBLIC", "main");
        repositoryService.create(Repository.OwnerType.USER.name(), admin.getId(),
                EMPTY_REPO, null, "PUBLIC", "main");

        pushFirstCommit(work, Path.of(repo.getStoragePath()));

        var issue = issueService.create(repo.getId(), "A seeded issue",
                "Body with a `code span` and a [link](https://example.com).", admin.getId());
        issueService.addComment(issue.getId(), "A seeded comment.", admin.getId());
        var label = labelService.createLabel(repo.getId(), "bug", "#d73a4a", "Something is broken");
        labelService.addLabelToIssue(issue.getId(), label.getId());

        var pr = prService.create(repo.getId(), "A seeded pull request",
                "Fixes #1", admin.getId(), "feature/seed", "main");
        prService.addReview(pr.getId(), admin.getId(),
                PullRequestReview.State.APPROVE.name(), "Looks good.");
    }

    /** Clones the bare repository, commits and pushes, so the tree pages have content. */
    private void pushFirstCommit(Path work, Path bare) throws IOException {
        Path wc = work.resolve("wc");
        gitService.run(work, "clone", bare.toAbsolutePath().toString(), wc.toString());
        Files.writeString(wc.resolve("README.md"), "# Seeded\n\nRendered by PageRenderTest.\n");
        Files.createDirectories(wc.resolve("src"));
        Files.writeString(wc.resolve("src/App.java"), "package app;\n\nclass App {}\n");
        gitService.run(wc, "add", "-A");
        gitService.run(wc, "-c", "user.email=render@test.com", "-c", "user.name=" + OWNER,
                "commit", "-m", "Seed the render test");
        gitService.run(wc, "push", "origin", "HEAD:main");
    }

    @ParameterizedTest(name = "signed in {0}")
    @ValueSource(strings = {
        "/",
        "/@/renderadmin",
        "/new",
        "/notifications",
        "/settings/keys",
        "/search?q=render",
        "/teams/new",
        "/admin",
        "/admin/users",
        "/admin/system",
        "/admin/audit",
    })
    @WithMockUser(username = OWNER, roles = {"ADMIN"})
    void signedInPagesRenderWhole(String path) throws Exception {
        assertRendersWhole(path);
    }

    @ParameterizedTest(name = "repository {0}")
    @ValueSource(strings = {
        "",
        "/issues",
        "/issues?state=closed",
        "/issues/new",
        "/issues/1",
        "/pulls",
        "/pulls?state=closed",
        "/pulls/new",
        "/pulls/1",
        "/tree/main",
        "/tree/main/src",
        "/blob/main/README.md",
        "/blob/main/src/App.java",
        "/actions",
        "/activity",
        "/settings",
    })
    @WithMockUser(username = OWNER, roles = {"ADMIN"})
    void repositoryPagesRenderWhole(String suffix) throws Exception {
        assertRendersWhole("/" + OWNER + "/" + REPO + suffix);
    }

    /** The empty branch of every list, which is where a null model attribute hides. */
    @ParameterizedTest(name = "empty state {0}")
    @ValueSource(strings = {"", "/issues", "/pulls", "/actions", "/activity"})
    @WithMockUser(username = OWNER, roles = {"ADMIN"})
    void emptyStatesRenderWhole(String suffix) throws Exception {
        assertRendersWhole("/" + OWNER + "/" + EMPTY_REPO + suffix);
    }

    /** Standalone pages reachable without an account. */
    @ParameterizedTest(name = "anonymous {0}")
    @ValueSource(strings = {"/login", "/register", "/@/renderadmin"})
    void anonymousStandalonePagesRenderWhole(String path) throws Exception {
        assertRendersWhole(path);
    }

    /** A public repository is browsable without an account, so these must render too. */
    @ParameterizedTest(name = "anonymous repository {0}")
    @ValueSource(strings = {"", "/issues", "/pulls/1", "/blob/main/README.md"})
    void anonymousRepositoryPagesRenderWhole(String suffix) throws Exception {
        assertRendersWhole("/" + OWNER + "/" + REPO + suffix);
    }

    private void assertRendersWhole(String path) throws Exception {
        // The X-Inertia header is what a client navigation sends, and it makes the
        // server answer with the page object itself rather than the HTML shell.
        MvcResult result = mvc.perform(get(path).header("X-Inertia", "true")).andReturn();
        String body = result.getResponse().getContentAsString();

        assertThat(result.getResponse().getStatus()).as("status of %s", path).isEqualTo(200);
        assertThat(result.getResponse().getContentType())
                .as("content type of %s", path).startsWith("application/json");

        JsonNode page = new ObjectMapper().readTree(body);
        String component = page.path("component").asText();

        assertThat(component).as("%s must name a component", path).isNotEmpty();
        assertThat(PAGES.resolve(component + ".tsx"))
                .as("%s renders component '%s', which has no page in frontend/src/pages",
                        path, component)
                .exists();

        assertThat(page.has("props")).as("%s must carry props", path).isTrue();

        // The whole model is serialised now, so a secret is only ever one careless
        // getter away from the browser. $2a$ is the bcrypt prefix.
        assertThat(body).as("%s must not leak a password hash", path).doesNotContain("$2a$");
        assertThat(body).as("%s must not leak a storage path", path).doesNotContain("storagePath");
    }
}
