package com.furimeo.gitkoo.workflow;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import com.furimeo.gitkoo.auth.UserService;
import com.furimeo.gitkoo.repository.Repository;
import com.furimeo.gitkoo.repository.RepositoryService;

/** Actions page showing workflow runs (DESIGN.md §12). */
@Controller
public class WorkflowController {

    private final WorkflowService workflowService;
    private final RepositoryService repositoryService;
    private final UserService userService;

    private final com.furimeo.gitkoo.repository.RepositoryPermissionService permissionService;

    private final com.furimeo.gitkoo.repository.RepoChrome repoChrome;

    public WorkflowController(WorkflowService workflowService, RepositoryService repositoryService,
                             UserService userService,
                             com.furimeo.gitkoo.repository.RepositoryPermissionService permissionService,
                                com.furimeo.gitkoo.repository.RepoChrome repoChrome) {
        this.workflowService = workflowService;
        this.repositoryService = repositoryService;
        this.userService = userService;
        this.permissionService = permissionService;
            this.repoChrome = repoChrome;
    }

    /**
     * Requires at least READ for the caller, else 403.
     *
     * <p>Build logs routinely contain paths, environment details and occasionally
     * secrets echoed by a step. This controller checked nothing, so those were
     * readable by any signed-in user for any repository, private ones included.
     */
    private void requireRead(org.springframework.security.core.userdetails.User principal, Repository repo) {
        var actor = principal == null || "anonymousUser".equals(principal.getUsername())
                ? null
                : userService.findByUsername(principal.getUsername()).orElse(null);
        if (!permissionService.hasPermission(actor, repo,
                com.furimeo.gitkoo.repository.RepositoryPermissionService.Permission.READ)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You do not have read access to " + repo.getName());
        }
    }

    @GetMapping("/{username}/{name}/actions")
    public String actions(@PathVariable String username, @PathVariable String name, Model model,
                          @org.springframework.security.core.annotation.AuthenticationPrincipal
                          org.springframework.security.core.userdetails.User principal) {
        Repository repo = resolveRepo(username, name);
        requireRead(principal, repo);
        List<WorkflowRun> runs = workflowService.listRuns(repo.getId());
        repoChrome.apply(model, username, repo,
                principal == null ? null : principal.getUsername());
        model.addAttribute("runs", runs);
        return "repository/actions";
    }

    /**
     * Streams the persisted log output for a single workflow run as plain text
     * (DESIGN.md §109 "workflow logs"). The run must belong to the resolved
     * repository, otherwise a 404 is returned.
     */
    @GetMapping(value = "/{username}/{name}/actions/{runId}/logs", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public ResponseEntity<String> logs(@PathVariable String username, @PathVariable String name,
                                       @PathVariable Long runId,
                                       @org.springframework.security.core.annotation.AuthenticationPrincipal
                                       org.springframework.security.core.userdetails.User principal) {
        Repository repo = resolveRepo(username, name);
        requireRead(principal, repo);
        return workflowService.findRun(runId)
                .filter(run -> run.getRepositoryId().equals(repo.getId()))
                .map(run -> ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN)
                        .body(workflowService.readLog(runId)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private Repository resolveRepo(String username, String name) {
        var owner = userService.findByUsername(username)
                .orElseThrow(() -> new com.furimeo.gitkoo.web.NotFoundException("User not found"));
        return repositoryService.findByOwnerAndName(Repository.OwnerType.USER.name(), owner.getId(), name)
                .orElseThrow(() -> new com.furimeo.gitkoo.web.NotFoundException("Repository not found"));
    }
}
