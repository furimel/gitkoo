package com.furimeo.gitkoo.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import com.furimeo.gitkoo.auth.User;
import com.furimeo.gitkoo.auth.UserService;
import com.furimeo.gitkoo.repository.Repository;
import com.furimeo.gitkoo.repository.RepositoryService;

/**
 * Submits forms over real HTTP, with the encoding the browser actually uses.
 *
 * <p>Written after every form in the application shipped broken. The controllers
 * read their input with {@code @RequestParam}, which the servlet container fills
 * from a parsed form body; the Inertia client sends JSON by default, and nothing
 * parses JSON into parameters - so each submission answered 400 with "Required
 * request parameter is not present". Nothing caught it: the render tests only issue
 * GETs, and the manual checks used curl's {@code -d}, which is form-encoded and
 * therefore worked.
 *
 * <p>So this runs a real server and writes real request bodies. MockMvc cannot
 * express the bug: its multipart builder takes parts and parameters separately and
 * never parses a body, so a test written against it would pass whatever the client
 * puts on the wire.
 *
 * <p>{@link #aJsonBodyIsRejected()} is the other half. It pins the reason the client
 * is configured the way it is: if someone removes {@code forceFormData}, that test
 * is the one that explains why the forms went quiet.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FormSubmissionTest {

    private static final String OWNER = "formadmin";
    private static final String PASSWORD = "password123";
    private static final String REPO = "form-repo";
    private static final String BOUNDARY = "----GitKooFormTestBoundary";

    @LocalServerPort private int port;
    @Autowired private UserService userService;
    @Autowired private RepositoryService repositoryService;

    /** Holds the session cookie and the CSRF cookie across requests, like a browser. */
    private HttpClient http;

    @BeforeAll
    void seed() throws Exception {
        User admin = userService.needsSetup()
                ? userService.createAdministrator(OWNER, "form@test.com", PASSWORD)
                : userService.findByUsername(OWNER).orElseGet(
                        () -> userService.createUser(OWNER, "form@test.com", PASSWORD));

        if (repositoryService.findByOwnerUsernameAndName(OWNER, REPO).isEmpty()) {
            repositoryService.create(Repository.OwnerType.USER.name(), admin.getId(),
                    REPO, "A repository used by the form test", "PUBLIC", "main");
        }

        http = HttpClient.newBuilder()
                /*
                 * ACCEPT_ALL, explicitly. The default policy requires the cookie's
                 * domain to match the origin server, and Java's implementation
                 * refuses a host with no dot in it - so every cookie from localhost
                 * is silently dropped and the session never forms.
                 */
                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        signIn();
    }

    @Test
    void createRepository() throws Exception {
        assertAccepted("/new", Map.of(
                "name", "from-a-form",
                "description", "created by the form test",
                "visibility", "PUBLIC",
                "defaultBranch", "main"));
    }

    @Test
    void openIssue() throws Exception {
        assertAccepted("/" + OWNER + "/" + REPO + "/issues/new", Map.of(
                "title", "An issue opened by the form test",
                "body", "With a body."));
    }

    @Test
    void createTeam() throws Exception {
        assertAccepted("/teams/new", Map.of(
                "name", "form-team",
                "displayName", "Form Team",
                "description", "created by the form test"));
    }

    @Test
    void repositorySettings() throws Exception {
        assertAccepted("/" + OWNER + "/" + REPO + "/settings", Map.of(
                "description", "edited by the form test",
                "defaultBranch", "main",
                "visibility", "PUBLIC"));
    }

    @Test
    void topics() throws Exception {
        assertAccepted("/" + OWNER + "/" + REPO + "/settings/topics",
                Map.of("topics", "java spring"));
    }

    /**
     * A POST carrying no fields - star, watch, close, mark read.
     *
     * <p>An empty FormData still produces a multipart body with nothing but the
     * closing boundary, and a container that rejected one would break every
     * one-click action in the application.
     */
    @Test
    void actionsWithNoFields() throws Exception {
        assertAccepted("/" + OWNER + "/" + REPO + "/star", Map.of());
        assertAccepted("/" + OWNER + "/" + REPO + "/watch", Map.of());
    }

    /**
     * The regression this suite exists for: a JSON body must not satisfy a handler
     * that reads request parameters.
     */
    @Test
    void aJsonBodyIsRejected() throws Exception {
        HttpResponse<String> response = send(HttpRequest.newBuilder(uri("/teams/new"))
                .header("Content-Type", "application/json")
                .header("X-XSRF-TOKEN", csrfToken())
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"name\":\"json-team\",\"displayName\":\"\",\"description\":\"\"}")));

        assertThat(response.statusCode())
                .as("a JSON body reaches no @RequestParam, so it must fail loudly - "
                        + "if this passes, the client may have stopped sending form data")
                .isEqualTo(400);
    }

    /**
     * The client half of the contract.
     *
     * <p>Everything above posts bytes this test writes itself, so it proves the
     * server accepts form data and rejects JSON - and would keep passing if the
     * client went back to sending JSON tomorrow. Reading the source is a blunt
     * instrument, but the line it pins is the one whose removal breaks every form in
     * the product, in a way that only shows up when a person tries to submit one.
     */
    @Test
    void theClientIsConfiguredToSendFormData() throws IOException {
        Path main = Path.of("frontend/src/main.tsx");
        assertThat(main).as("the client entry point should exist").exists();
        assertThat(Files.readString(main))
                .as("main.tsx must set forceFormData, and it must be inside "
                        + "createInertiaApp's `defaults`: that call begins with "
                        + "config.replace(defaults) and silently discards anything "
                        + "configured before it. Without the setting the client sends "
                        + "JSON, no @RequestParam is populated, and every form answers 400")
                .containsPattern("defaults:\\s*\\{visitOptions[^}]*forceFormData: true");
    }

    // ── plumbing ────────────────────────────────────────────────────────

    private void signIn() throws Exception {
        // Touch a page first so Spring Security issues the XSRF-TOKEN cookie.
        send(HttpRequest.newBuilder(uri("/login")).GET());

        HttpResponse<String> response = send(HttpRequest.newBuilder(uri("/login"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("X-XSRF-TOKEN", csrfToken())
                .POST(HttpRequest.BodyPublishers.ofString(
                        "username=" + OWNER + "&password=" + PASSWORD)));

        assertThat(response.statusCode()).as("sign-in should redirect").isBetween(300, 399);
        assertThat(response.headers().firstValue("location").orElse(""))
                .as("sign-in should not bounce back to the login page")
                .doesNotContain("error");
    }

    /**
     * Posts the fields as {@code multipart/form-data} and requires a redirect.
     *
     * <p>4xx is the failure this exists to catch. A 200 would mean the handler
     * re-rendered instead of acting, which for these routes is also wrong.
     */
    private void assertAccepted(String path, Map<String, String> fields) throws Exception {
        Map<String, String> ordered = new LinkedHashMap<>(fields);
        HttpResponse<String> response = send(HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "multipart/form-data; boundary=" + BOUNDARY)
                .header("X-XSRF-TOKEN", csrfToken())
                .POST(HttpRequest.BodyPublishers.ofByteArray(multipartBody(ordered))));

        assertThat(response.statusCode())
                .as("POST %s with %s as multipart/form-data; body was %s",
                        path, ordered.keySet(), abbreviate(response.body()))
                .isBetween(300, 399);
    }

    /** The exact bytes a browser writes for a form of plain text inputs. */
    private static byte[] multipartBody(Map<String, String> fields) {
        StringBuilder body = new StringBuilder();
        fields.forEach((name, value) -> body
                .append("--").append(BOUNDARY).append("\r\n")
                .append("Content-Disposition: form-data; name=\"").append(name).append("\"\r\n\r\n")
                .append(value).append("\r\n"));
        body.append("--").append(BOUNDARY).append("--\r\n");
        return body.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * The CSRF token, read from the cookie the way the client does.
     *
     * <p>Spring Security's cookie repository is deliberately not http-only so the
     * browser can echo it in a header; this does the same.
     *
     * <p>Fetches a page when the cookie is absent, because signing in deletes it:
     * {@code CsrfAuthenticationStrategy} discards the anonymous token on a successful
     * authentication, and the replacement is only issued on the next request. A
     * browser hits that same gap and never notices - it always loads a page before
     * submitting a form.
     */
    private String csrfToken() throws IOException, InterruptedException {
        String token = storedToken();
        if (token == null) {
            send(HttpRequest.newBuilder(uri("/")).GET());
            token = storedToken();
        }
        if (token == null) {
            throw new IllegalStateException("No XSRF-TOKEN cookie was issued");
        }
        return token;
    }

    private String storedToken() {
        CookieManager cookies = (CookieManager) http.cookieHandler().orElseThrow();
        return cookies.getCookieStore().getCookies().stream()
                .filter(cookie -> "XSRF-TOKEN".equals(cookie.getName()))
                .map(java.net.HttpCookie::getValue)
                .filter(value -> !value.isEmpty())
                .findFirst()
                .orElse(null);
    }

    private HttpResponse<String> send(HttpRequest.Builder request) throws IOException, InterruptedException {
        return http.send(request.timeout(Duration.ofSeconds(20)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private static String abbreviate(String body) {
        if (body == null || body.isBlank()) {
            return "(empty)";
        }
        String flat = String.join(" ", List.of(body.strip().split("\\s+")));
        return flat.length() > 300 ? flat.substring(0, 300) + "..." : flat;
    }
}
